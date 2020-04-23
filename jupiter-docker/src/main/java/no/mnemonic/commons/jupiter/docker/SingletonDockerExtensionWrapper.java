package no.mnemonic.commons.jupiter.docker;

import no.mnemonic.commons.utilities.StringUtils;
import org.junit.jupiter.api.extension.*;

/**
 * {@link DockerExtension} will create and destroy a Docker container once per test class which means having multiple
 * test classes using the same {@link DockerExtension} within a single test execution can cause high resource consumption.
 * This wrapper makes sure that a single instance of the same Docker container is only started once per test execution.
 * <p>
 * <b>WARNING:</b> This extension does not call {@link DockerExtension#afterAll(ExtensionContext)} after the completion
 * of tests. The wrapped extension <em>must</em> register a JVM shutdown hook (default in {@link DockerExtension}) which
 * shuts down the started container.
 * <p>
 * Initialize SingletonDockerExtensionWrapper in the following way using {@link RegisterExtension}:
 * <pre>
 * {@code @RegisterExtension
 * public static SingletonDockerExtensionWrapper<DockerExtension> docker = SingletonDockerExtensionWrapper.builder()
 *   .setDockerExtension(DockerExtension.builder()
 *     .setImageName("busybox")
 *     .setReachabilityTimeout(30)
 *     .addApplicationPort(8080)
 *     .build())
 *   .build();}
 * </pre>
 * <p>
 * SingletonDockerExtensionWrapper <em>must</em> be defined in a parent class all test classes using the same Docker
 * container <em>must</em> inherit from. It will not work if SingletonDockerExtensionWrapper is defined multiple times
 * in independent test classes. The created SingletonDockerExtensionWrapper exposes {@link #getDockerExtension()} for
 * retrieving the underlying {@link DockerExtension} which can be referenced in tests.
 *
 * @param <T> Type of wrapped {@link DockerExtension}
 */
public class SingletonDockerExtensionWrapper<T extends DockerExtension>
        implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

  private final T dockerExtension;

  private SingletonDockerExtensionWrapper(T dockerExtension) {
    if (dockerExtension == null) throw new IllegalArgumentException("'dockerExtension' not provided!");
    this.dockerExtension = dockerExtension;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    // Only call beforeAll() of the wrapped DockerExtension if the container hasn't been initialized yet.
    // This makes sure that the container will only be started once.
    if (StringUtils.isBlank(dockerExtension.getContainerID())) {
      dockerExtension.beforeAll(context);
    }
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    // Always call beforeEach() of wrapped DockerExtension.
    dockerExtension.beforeEach(context);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    // Always call afterEach() of wrapped DockerExtension.
    dockerExtension.afterEach(context);
  }

  /**
   * Return wrapped {@link DockerExtension}.
   *
   * @return Wrapped {@link DockerExtension}
   */
  public T getDockerExtension() {
    return dockerExtension;
  }

  /**
   * Create builder for {@link SingletonDockerExtensionWrapper}.
   *
   * @return Builder object
   */
  public static <T extends DockerExtension> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Builder to create a {@link SingletonDockerExtensionWrapper}.
   */
  public static class Builder<T extends DockerExtension> {
    private T dockerExtension;

    /**
     * Build a {@link SingletonDockerExtensionWrapper} instance wrapping another {@link DockerExtension}.
     *
     * @return Configured {@link SingletonDockerExtensionWrapper}
     */
    public SingletonDockerExtensionWrapper<T> build() {
      return new SingletonDockerExtensionWrapper<>(dockerExtension);
    }

    /**
     * Set {@link DockerExtension} which will be wrapped as a singleton.
     *
     * @param dockerExtension Preconfigured {@link DockerExtension} which will be wrapped
     */
    public Builder<T> setDockerExtension(T dockerExtension) {
      this.dockerExtension = dockerExtension;
      return this;
    }
  }
}
