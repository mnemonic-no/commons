package no.mnemonic.commons.jupiter.docker;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.MapUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.commons.utilities.lambda.LambdaUtils;
import org.junit.jupiter.api.extension.*;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.builder.resteasy.ResteasyDockerClientBuilder;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerConfig;
import org.mandas.docker.client.messages.ContainerInfo;
import org.mandas.docker.client.messages.HostConfig;
import org.mandas.docker.client.messages.PortBinding;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.ObjectUtils.ifNull;
import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;

/**
 * DockerExtension is a JUnit5 extension which starts up an isolated Docker container in a unit test, for example for
 * integration tests against an external database. DockerExtension will start up the container only once before all
 * tests and will make sure that the container is teared down after all tests (i.e. when the JVM shuts down).
 * <p>
 * In order to start up a container the following steps are performed:
 * <ol>
 * <li>Initialize a Docker client. If the $DOCKER_HOST environment variable is set it will connect to the Docker
 * installation specified by this variable. Otherwise it will try to connect to localhost on TCP port 2375 (default
 * Docker daemon port).</li>
 * <li>Initialize and start up a Docker container specified by the name of a Docker image. It is expected that the
 * Docker image is already installed (for example by performing 'docker pull').</li>
 * <li>Test that the container is reachable. See {@link #isContainerReachable()} for more information.</li>
 * <li>Prepare the container with additional data. See {@link #prepareContainer()} for more information.</li>
 * </ol>
 * <p>
 * After all tests are finished, either successfully, with an exception or by user cancellation, the running container
 * is stopped and removed in order to not leave stale containers behind.
 * <p>
 * Initialize DockerExtension in the following way using {@link RegisterExtension}:
 * <pre>
 * {@code @RegisterExtension
 * public static DockerExtension docker = DockerExtension.builder()
 *   .setImageName("busybox")
 *   .setReachabilityTimeout(30)
 *   .addApplicationPort(8080)
 *   .build();}
 * </pre>
 * See {@link DockerExtension.Builder} for more information on the configuration properties.
 * <p>
 * This class provides a basic Docker extension but it is most useful to extend it and override {@link #isContainerReachable()}
 * and {@link #prepareContainer()} for more specific use cases, for instance when testing a specific database.
 * See {@link CassandraDockerExtension} as an example.
 * <p>
 * <b>Proxy settings</b>
 * <p>
 * The DockerExtension will by default use system properties to determine proxy settings when communicating with the
 * docker daemon. To completely disable proxy, you can set the system property "-DDockerExtension.disable.proxy=true".
 */
public class DockerExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

  private static final String DOCKER_HOST_ENVIRONMENT_VARIABLE = "DOCKER_HOST";
  private static final int DEFAULT_DOCKER_DAEMON_PORT = 2375;
  private static final int DEFAULT_STOP_TIMEOUT_SECONDS = 10;
  private static final int DEFAULT_REACHABILITY_TIMEOUT_SECONDS = 30;

  private final String imageName;
  private final Set<String> applicationPorts;
  private final String exposedPortsRange;
  private final int reachabilityTimeout;
  private final boolean skipReachabilityCheck;
  private final boolean skipPullDockerImage;
  private final Supplier<DockerClient> dockerClientResolver;
  private final Map<String, String> environmentVariables;

  private DockerClient docker;
  private String containerID;

  /**
   * Constructor to override by subclasses.
   *
   * @param imageName             Name of Docker image (required)
   * @param applicationPorts      Application ports available inside the container (at least one is required)
   * @param exposedPortsRange     Range of ports for mapping to the outside of the container (optional)
   * @param reachabilityTimeout   Timeout until testing that container is reachable stops (optional)
   * @param skipReachabilityCheck If set skip testing that container is reachable (optional)
   * @param skipPullDockerImage   If set skip pulling docker image (optional)
   * @param dockerClientResolver  Function to resolve DockerClient (optional)
   * @param environmentVariables  Container's environment variables (optional)
   * @throws IllegalArgumentException If one of the required parameters is not provided
   */
  protected DockerExtension(String imageName,
                            Set<Integer> applicationPorts,
                            String exposedPortsRange,
                            int reachabilityTimeout,
                            boolean skipReachabilityCheck,
                            boolean skipPullDockerImage,
                            Supplier<DockerClient> dockerClientResolver,
                            Map<String, String> environmentVariables) {
    if (StringUtils.isBlank(imageName)) throw new IllegalArgumentException("'imageName' not provided!");
    if (CollectionUtils.isEmpty(applicationPorts))
      throw new IllegalArgumentException("'applicationPorts' not provided!");
    if (!skipReachabilityCheck && reachabilityTimeout <= 0)
      throw new IllegalArgumentException("'reachabilityTimeout' not provided!");

    this.imageName = imageName;
    this.applicationPorts = Collections.unmodifiableSet(applicationPorts.stream()
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .collect(Collectors.toSet()));
    this.exposedPortsRange = exposedPortsRange;
    this.reachabilityTimeout = reachabilityTimeout;
    this.skipPullDockerImage = skipPullDockerImage;
    this.skipReachabilityCheck = skipReachabilityCheck;
    this.dockerClientResolver = ifNull(dockerClientResolver, (Supplier<DockerClient>) this::resolveDockerClient);
    this.environmentVariables = Collections.unmodifiableMap(environmentVariables);

    // Make sure to always shutdown any containers in order to not leave stale containers on the host machine,
    // e.g. in case of exceptions or when the user stops the tests. This won't work if the JVM process is killed.
    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownContainer));
  }

  /**
   * Returns the host where the started Docker container is available. It takes the $DOCKER_HOST environment variable
   * into account and falls back to 'localhost' if the variable is not specified.
   *
   * @return Host where the started Docker container is available
   */
  public String getExposedHost() {
    return DockerTestUtils.getDockerHost();
  }

  /**
   * DockerResource will map the application ports, which are the ports applications listen to inside the container,
   * to random ports on the host machine, which can be used to communicate with the applications. For example,
   * the application port 8080 could be mapped to random port 33333 on the host machine.
   * <p>
   * This method returns the exposed host port for a given application port.
   *
   * @param applicationPort Application port inside container
   * @return Exposed port on host machine
   * @throws IllegalStateException If mapped host port cannot be determined
   */
  public int getExposedHostPort(int applicationPort) {
    int hostPort;

    try {
      // Fetch container information and find binding for application port which contains the exposed host port.
      ContainerInfo info = docker.inspectContainer(containerID);
      // Return first available TCP host port bound to application port.
      hostPort = map(info.networkSettings().ports())
              .getOrDefault(applicationPort + "/tcp", list())
              .stream()
              .map(PortBinding::hostPort)
              .map(Integer::parseInt)
              .findFirst()
              .orElse(0);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not determine exposed host port.", ex);
    }

    if (hostPort <= 0) {
      throw new IllegalStateException("Could not determine exposed host port.");
    }

    return hostPort;
  }

  /**
   * Create builder for DockerExtension.
   *
   * @return Builder object
   */
  public static <T extends Builder<?>> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Subclasses can override this method in order to apply additional configuration to the host inside the container.
   * <p>
   * If not overridden the default configuration will be used.
   *
   * @param config Default configuration as set up by DockerExtension
   * @return Modified host configuration
   */
  protected HostConfig additionalHostConfig(HostConfig config) {
    // By default, return host configuration unchanged.
    return config;
  }

  /**
   * Subclasses can override this method in order to apply additional configuration to the container itself.
   * <p>
   * If not overridden the default configuration will be used.
   *
   * @param config Default configuration as set up by DockerExtension
   * @return Modified container configuration
   */
  protected ContainerConfig additionalContainerConfig(ContainerConfig config) {
    // By default, return container configuration unchanged.
    return config;
  }

  /**
   * Subclasses can override this method in order to implement a check to determine if a container is reachable. After
   * start up of the container this method will be called until it either returns true or 'reachabilityTimeout' is
   * reached. If the container is not reachable until 'reachabilityTimeout' starting up DockerExtension will fail with
   * a TimeoutException.
   * <p>
   * If not overridden the method immediately returns true.
   *
   * @return True if container is reachable
   */
  protected boolean isContainerReachable() {
    // By default, just return true.
    return true;
  }

  /**
   * Subclasses can override this method in order to prepare a container once before tests are executed, for example by
   * initializing a database with a schema or inserting some application data into a database. This method is called
   * once after it was determined that the container is reachable by {@link #isContainerReachable()}.
   * <p>
   * If not overridden the method does nothing.
   */
  protected void prepareContainer() {
    // By default, do nothing.
  }

  /**
   * Expose DockerClient used by DockerExtension to subclasses. Use this client when overriding {@link #isContainerReachable()}
   * or {@link #prepareContainer()}.
   *
   * @return DockerClient used by DockerExtension
   */
  public DockerClient getDockerClient() {
    return docker;
  }

  /**
   * Expose containerID of the started container to subclasses. Use this containerID when overriding {@link #isContainerReachable()}
   * and {@link #prepareContainer()} in order to communicate with the started container directly.
   *
   * @return containerID of started container
   */
  public String getContainerID() {
    return containerID;
  }

  /**
   * Initialize DockerExtension before executing tests. It should not be necessary to override this method.
   *
   * @throws Exception If initialization fails
   */
  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    synchronized (DockerExtension.class) {
      // Only initialize everything once. It will be automatically teared down when the JVM shuts down.
      if (docker == null) {
        initializeDockerClient();
        pullDockerImage();
        initializeContainer();
        testContainerReachability();
        prepareContainer();
      }
    }
  }

  /**
   * Subclasses can override this method in order to implement specific @BeforeEach behaviour.
   */
  @Override
  public void beforeEach(ExtensionContext context) {
    // By default, do nothing. DockerExtension must implement this method such
    // that SingletonDockerExtensionWrapper can handle @BeforeEach properly.
  }

  /**
   * Subclasses can override this method in order to implement specific @AfterEach behaviour.
   */
  @Override
  public void afterEach(ExtensionContext context) {
    // By default, do nothing. DockerExtension must implement this method such
    // that SingletonDockerExtensionWrapper can handle @AfterEach properly.
  }

  /**
   * Teardown DockerExtension after executing tests. It should not be necessary to override this method.
   */
  @Override
  public void afterAll(ExtensionContext context) {
    synchronized (DockerExtension.class) {
      if (docker != null) {
        shutdownContainer();
        docker = null;
      }
    }
  }

  private DockerClient resolveDockerClient() {
    try {
      if (!StringUtils.isBlank(System.getenv(DOCKER_HOST_ENVIRONMENT_VARIABLE))) {
        // If DOCKER_HOST is set create docker client from environment variables.
        return new ResteasyDockerClientBuilder()
                .fromEnv()
                .useProxy(useProxySettings())
                .build();
      } else {
        // Otherwise connect to localhost on the default daemon port.
        return new ResteasyDockerClientBuilder()
                .uri(String.format("http://localhost:%d", DEFAULT_DOCKER_DAEMON_PORT))
                .useProxy(useProxySettings())
                .build();
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Could not create docker client.", ex);
    }
  }

  private boolean useProxySettings() {
    // Allow user to turn off proxy autodetection by setting this property using a system property.
    return !Boolean.parseBoolean(ifNull(System.getProperty("DockerExtension.disable.proxy"), "false"));
  }

  private void initializeDockerClient() {
    this.docker = dockerClientResolver.get();

    try {
      // Check that docker daemon is reachable.
      if (!"OK".equals(docker.ping())) {
        throw new IllegalStateException("ping() did not return OK.");
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Could not connect to docker daemon.", ex);
    }
  }

  private void pullDockerImage() {
    if(skipPullDockerImage) {
      return;
    }
    try {
      docker.pull(imageName);
    } catch (DockerException | InterruptedException e) {
      throw new IllegalStateException(String.format("Could not pull docker image '%s'", imageName), e);
    }
  }

  private void initializeContainer() {
    PortBinding portBinding = StringUtils.isBlank(exposedPortsRange) ? PortBinding.randomPort("0.0.0.0") :
            PortBinding.of("0.0.0.0", exposedPortsRange);

    // Bind ports on the host to the application ports of the container randomly or with configured range.
    // Also apply any additional host configuration by calling additionalHostConfig().
    HostConfig hostConfig = additionalHostConfig(HostConfig.builder()
            .portBindings(map(applicationPorts, port -> T(port, list(portBinding))))
            .build());
    // Convert provided environmental variables to appropriate docker format.
    List<String> env = environmentVariables.entrySet()
            .stream()
            .map(e -> String.format("%s=%s", e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    // Configure container with the image to start and host port -> application port bindings.
    // Also apply any additional container configuration by calling additionalContainerConfig().
    ContainerConfig containerConfig = additionalContainerConfig(ContainerConfig.builder()
            .image(imageName)
            .exposedPorts(applicationPorts)
            .hostConfig(hostConfig)
            .env(env)
            .build());

    try {
      containerID = docker.createContainer(containerConfig).id();
      docker.startContainer(containerID);
    } catch (Exception ex) {
      throw new IllegalStateException(String.format("Could not start container (image '%s').", imageName), ex);
    }
  }

  private void shutdownContainer() {
    // Ignore exceptions because the container goes down anyways.
    LambdaUtils.tryTo(() -> {
      if (docker == null || StringUtils.isBlank(containerID)) return;
      docker.stopContainer(containerID, DEFAULT_STOP_TIMEOUT_SECONDS);
      docker.removeContainer(containerID);
    });

    // But always release client connection.
    ObjectUtils.ifNotNullDo(docker, DockerClient::close);
  }

  private void testContainerReachability() throws Exception {
    if (skipReachabilityCheck) return;

    if (!LambdaUtils.waitFor(this::isContainerReachable, reachabilityTimeout, TimeUnit.SECONDS)) {
      throw new TimeoutException("Could not connect to container before timeout.");
    }
  }

  /**
   * Builder to create a DockerExtension.
   * <p>
   * Subclasses of DockerExtension can also define own builders extending this builder in order to be able to configure
   * the same properties. The configurable properties are exposed as protected fields which can be passed to the
   * constructor of a subclass. This constructor in turn should pass them to the constructor of DockerExtension.
   * See {@link CassandraDockerExtension.Builder} as an example.
   */
  public static class Builder<T extends Builder<?>> {
    protected String imageName;
    protected Set<Integer> applicationPorts;
    protected String exposedPortsRange;
    protected int reachabilityTimeout = DEFAULT_REACHABILITY_TIMEOUT_SECONDS;
    protected boolean skipReachabilityCheck;
    protected boolean skipPullDockerImage = false;
    protected Supplier<DockerClient> dockerClientResolver;
    protected Map<String, String> environmentVariables = new HashMap<>();

    /**
     * Build a configured DockerExtension.
     *
     * @return Configured DockerExtension
     */
    public DockerExtension build() {
      return new DockerExtension(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout,
              skipReachabilityCheck, skipPullDockerImage, dockerClientResolver, environmentVariables);
    }

    /**
     * Set image name of container to use. The image must be available in Docker, it is not automatically pulled!
     *
     * @param imageName Image name
     * @return Builder
     */
    public T setImageName(String imageName) {
      this.imageName = imageName;
      return (T) this;
    }

    /**
     * Set application ports which will be used inside the container and exposed outside of the container by mapping to
     * ports inside the range specified with {@link #setExposedPortsRange(String)} or random ports.
     * <p>
     * Also see {@link #getExposedHostPort(int)} for more information.
     *
     * @param applicationPorts Set of application ports
     * @return Builder
     */
    public T setApplicationPorts(Set<Integer> applicationPorts) {
      this.applicationPorts = applicationPorts;
      return (T) this;
    }

    /**
     * Add a single application port which will be used inside the container and exposed outside of the container by
     * mapping to a port inside the range specified with {@link #setExposedPortsRange(String)} or a random port.
     * <p>
     * Also see {@link #getExposedHostPort(int)} for more information.
     *
     * @param applicationPort Single application port
     * @return Builder
     */
    public T addApplicationPort(int applicationPort) {
      this.applicationPorts = SetUtils.addToSet(this.applicationPorts, applicationPort);
      return (T) this;
    }

    /**
     * Set port range which will be used for exposing ports inside the container to the outside of the container.
     *
     * @param exposedPortsRange String in format "firstPort-lastPort" which is used for setting a range of ports
     * @return Builder
     */
    public T setExposedPortsRange(String exposedPortsRange) {
      this.exposedPortsRange = exposedPortsRange;
      return (T) this;
    }

    /**
     * Set timeout in seconds until test for container reachability stops. Defaults to 30 seconds if not set.
     * <p>
     * Also see {@link #isContainerReachable()} for more information.
     *
     * @param reachabilityTimeout Timeout in seconds
     * @return Builder
     */
    public T setReachabilityTimeout(int reachabilityTimeout) {
      this.reachabilityTimeout = reachabilityTimeout;
      return (T) this;
    }

    /**
     * Configure DockerExtension to skip test for container reachability. Useful if application code implements similar functionality.
     *
     * @return Builder
     */
    public T skipReachabilityCheck() {
      this.skipReachabilityCheck = true;
      return (T) this;
    }

    /**
     * Skip pulling the image if set to true. Default is to pull the image before running
     * @param skipPullDockerImage whether to pull the image
     * @return Builder
     */
    public T setSkipPullDockerImage(boolean skipPullDockerImage) {
      this.skipPullDockerImage = skipPullDockerImage;
      return (T) this;
    }

    /**
     * Override the default behaviour of how a DockerClient will be created by providing a custom resolver function.
     * Should be used with care, but useful for providing a mock during unit testing, for instance.
     *
     * @param dockerClientResolver Customer DockerClient resolver function
     * @return Builder
     */
    public T setDockerClientResolver(Supplier<DockerClient> dockerClientResolver) {
      this.dockerClientResolver = dockerClientResolver;
      return (T) this;
    }

    /**
     * Set multiple environment variables for the container.
     *
     * @param variables Array of key-value pairs
     * @return Builder
     */
    public T setEnvironmentVariables(MapUtils.Pair<String, String>... variables) {
      this.environmentVariables = MapUtils.map(variables);
      return (T) this;
    }

    /**
     * Add an additional environment variable for the container.
     *
     * @param key   Variable name
     * @param value Variable value
     * @return Builder
     */
    public T addEnvironmentVariable(String key, String value) {
      this.environmentVariables = MapUtils.addToMap(this.environmentVariables, key, value);
      return (T) this;
    }
  }
}
