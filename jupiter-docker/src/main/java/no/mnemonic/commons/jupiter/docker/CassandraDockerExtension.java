package no.mnemonic.commons.jupiter.docker;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.messages.HostConfig;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static no.mnemonic.commons.utilities.collections.MapUtils.Pair.T;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;
import static org.mandas.docker.client.DockerClient.ExecCreateParam.*;

/**
 * CassandraDockerExtension is a JUnit5 extension which can be used to write integration tests against a Cassandra server
 * executed inside an isolated Docker container. It extends the basic {@link DockerExtension} and makes sure that the
 * container initialization waits until Cassandra is available. It is also possible to initialize Cassandra with a
 * schema and data by providing a CQL start up script. In addition, data stored into Cassandra will be automatically
 * truncated after each test if the extension was initialized with a truncate CQL script.
 * <p>
 * Initialize CassandraDockerExtension in the following way using {@link RegisterExtension}:
 * <pre>
 * {@code @RegisterExtension
 * public static CassandraDockerExtension cassandra = CassandraDockerExtension.builder()
 *   .setImageName("cassandra")
 *   .addApplicationPort(9042)
 *   .setSetupScript("setup.cql")
 *   .setTruncateScript("truncate.cql")
 *   .build();}
 * </pre>
 * See {@link DockerExtension.Builder} and {@link CassandraDockerExtension.Builder} for more information on the
 * configuration properties.
 */
public class CassandraDockerExtension extends DockerExtension {

  private static final Logger LOGGER = Logging.getLogger(CassandraDockerExtension.class);

  private final Path setupScript;
  private final Path truncateScript;

  private CassandraDockerExtension(String imageName,
                                   Set<Integer> applicationPort,
                                   String exposedPortsRange,
                                   int reachabilityTimeout,
                                   boolean skipReachabilityCheck,
                                   boolean skipPullDockerImage,
                                   Supplier<DockerClient> dockerClientResolver,
                                   String setupScript,
                                   String truncateScript,
                                   Map<String, String> environmentVariables) {
    super(imageName, applicationPort, exposedPortsRange, reachabilityTimeout, skipReachabilityCheck,
            skipPullDockerImage, dockerClientResolver, environmentVariables);

    // Both parameters are optional.
    this.setupScript = !StringUtils.isBlank(setupScript) ? checkFileExists(setupScript) : null;
    this.truncateScript = !StringUtils.isBlank(truncateScript) ? checkFileExists(truncateScript) : null;
  }

  /**
   * Create builder for CassandraDockerExtension.
   *
   * @return Builder object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Truncate data stored inside Cassandra after each test by executing the truncate CQL script.
   *
   * @throws IllegalStateException If CQL script could not be executed
   */
  @Override
  public void afterEach(ExtensionContext context) {
    ObjectUtils.ifNotNullDo(truncateScript, this::executeCqlScript);
  }

  /**
   * Adds Cassandra specific host configuration to default configuration from {@link DockerExtension}.
   *
   * @param config Default configuration as set up by DockerExtension
   * @return Modified host configuration
   */
  @Override
  protected HostConfig additionalHostConfig(HostConfig config) {
    return config.toBuilder()
            // Write Cassandra data to tmpfs in order to speed up writes.
            .tmpfs(map(T("/var/lib/cassandra", "")))
            // Deactivate swap because it will kill performance.
            .memorySwappiness(0)
            .build();
  }

  /**
   * Verifies that Cassandra is reachable by issuing a simple cqlsh command inside the Cassandra Docker container.
   *
   * @return True if cqlsh command returns successfully
   * @throws IllegalStateException If cqlsh command could not be executed
   */
  @Override
  protected boolean isContainerReachable() {
    try {
      // Execute a simple CQL command against cqlsh to test for reachability.
      // Workaround for https://github.com/spotify/docker-client/issues/513: also attach stdin.
      String id = getDockerClient().execCreate(getContainerID(), new String[]{"cqlsh", "-e", "describe cluster"},
              attachStdout(), attachStderr(), attachStdin()).id();
      String output = getDockerClient().execStart(id).readFully();
      // If the output contains the phrase "Connection error" Cassandra is not yet reachable.
      if (StringUtils.isBlank(output) || output.contains("Connection error")) {
        return false;
      }
    } catch (Exception ex) {
      LOGGER.warning(ex, "Could not execute 'cqlsh' to test for Cassandra reachability.");
      return false;
    }

    return true;
  }

  /**
   * Initializes Cassandra by executing the set up CQL script.
   *
   * @throws IllegalStateException If CQL script could not be executed
   */
  @Override
  protected void prepareContainer() {
    copyFilesToContainer();
    ObjectUtils.ifNotNullDo(setupScript, this::executeCqlScript);
  }

  private Path checkFileExists(String fileName) {
    URL fileUrl = ClassLoader.getSystemResource(fileName);
    if (fileUrl == null || !Files.isReadable(Paths.get(fileUrl.getPath()))) {
      throw new IllegalArgumentException(String.format("Cannot read '%s'!", fileName));
    }

    return Paths.get(fileUrl.getPath());
  }

  private void copyFilesToContainer() {
    try {
      // Copy start up script and truncate script to the container's /tmp/ folder.
      // Need to specify the parent folder where the file resides. This will copy all files in that folder.
      if (setupScript != null) {
        getDockerClient().copyToContainer(setupScript.getParent(), getContainerID(), "/tmp/");
      }
      if (truncateScript != null) {
        getDockerClient().copyToContainer(truncateScript.getParent(), getContainerID(), "/tmp/");
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Could not copy files to container.", ex);
    }
  }

  private void executeCqlScript(Path script) {
    String output;

    try {
      // Workaround for https://github.com/spotify/docker-client/issues/513: also attach stdin.
      String id = getDockerClient().execCreate(getContainerID(), new String[]{"cqlsh", "-f", "/tmp/" + script.getFileName()},
              attachStdout(), attachStderr(), attachStdin()).id();
      output = getDockerClient().execStart(id).readFully();
    } catch (Exception ex) {
      throw new IllegalStateException(String.format("Could not execute CQL script %s.", script.getFileName()), ex);
    }

    if (!StringUtils.isBlank(output)) {
      throw new IllegalStateException(String.format("Evaluation of CQL script %s failed.%n%s", script.getFileName(), output));
    }
  }

  /**
   * Builder to create a CassandraDockerExtension which extends {@link DockerExtension.Builder}.
   */
  public static class Builder extends DockerExtension.Builder<Builder> {
    private String setupScript;
    private String truncateScript;
    /**
     * Build a configured CassandraDockerExtension.
     *
     * @return Configured CassandraDockerExtension
     */
    @Override
    public CassandraDockerExtension build() {
      return new CassandraDockerExtension(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout,
              skipReachabilityCheck, skipPullDockerImage, dockerClientResolver, setupScript, truncateScript, environmentVariables);
    }

    /**
     * Set file name of CQL start up script. The file needs to be available on the classpath usually from the test
     * resources folder. Providing a start up script is optional.
     *
     * @param setupScript File name of start up script
     * @return Builder
     */
    public Builder setSetupScript(String setupScript) {
      this.setupScript = setupScript;
      return this;
    }

    /**
     * Set file name of CQL truncate script. The file needs to be available on the classpath usually from the test
     * resources folder. Providing a truncate script is optional.
     *
     * @param truncateScript File name of truncate script
     * @return Builder
     */
    public Builder setTruncateScript(String truncateScript) {
      this.truncateScript = truncateScript;
      return this;
    }
  }
}
