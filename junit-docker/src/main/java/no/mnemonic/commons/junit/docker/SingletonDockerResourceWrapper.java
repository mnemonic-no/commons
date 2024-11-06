package no.mnemonic.commons.junit.docker;

import no.mnemonic.commons.utilities.StringUtils;
import org.junit.rules.ExternalResource;

/**
 * {@link DockerResource} is created with intention to be used as a {@link org.junit.ClassRule}.
 * Since {@link org.junit.ClassRule} is executed once per test class, having multiple test classes within single test
 * execution can cause high resource consumption (starting and stopping Docker container can have great impact on system
 * resources). This wrapper makes sure that single instance of Docker container, defined in wrapped resource, is started
 * just once.
 * <p>
 * <b>WARNING:</b> This resource does not call after() after completion of tests.
 * Underlying resource should register JVM shutdown hook (default in DockerResource), which is used for shutting down of container.
 * <p>
 * After all tests are finished, either successfully, with an exception or by user cancellation, the wrapped resource
 * will cleanup stale container.
 * <p>
 * Initialize SingletonDockerResourceWrapper in the following way as {@link org.junit.ClassRule}:
 * <pre>
 * {@code @ClassRule
 *  public static SingletonDockerResourceWrapper<DockerResource> docker = SingletonDockerResourceWrapper.builder()
 *  .setDockerResource(DockerResource.builder()
 *     .setImageName("busybox")
 *     .setReachabilityTimeout(30)
 *     .addApplicationPort(8080)
 *     .build())
 *   .build();}
 * </pre>
 * <p>
 * Created SingletonDockerResourceWrapper exposes getDockerResource method for retrieving underlying {@link DockerResource}
 * which can be used in test.
 *
 * @param <T> wrapped {@link DockerResource} type
 *
 * @deprecated Use jupiter-docker instead
 */
@Deprecated
public class SingletonDockerResourceWrapper<T extends DockerResource> extends ExternalResource {

  private final T dockerResource;

  private SingletonDockerResourceWrapper(T dockerResource) {
    if (dockerResource == null) throw new IllegalArgumentException("dockerResource was null");
    this.dockerResource = dockerResource;
  }

  @Override
  protected void before() throws Throwable {
    if (StringUtils.isBlank(dockerResource.getContainerID())) {
      dockerResource.before();
    }
  }

  /**
   * Return wrapped {@link DockerResource}.
   *
   * @return Wrapped {@link DockerResource}
   */
  public T getDockerResource() {
    return dockerResource;
  }

  /**
   * Create builder for {@link SingletonDockerResourceWrapper}.
   *
   * @return Builder object
   */
  public static <T extends DockerResource> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Builder to create a {@link SingletonDockerResourceWrapper}.
   */
  public static class Builder<T extends DockerResource> {
    private T dockerResource;

    /**
     * Build a {@link SingletonDockerResourceWrapper} instance wrapping another {@link DockerResource}.
     *
     * @return Configured {@link SingletonDockerResourceWrapper}
     */
    public SingletonDockerResourceWrapper<T> build() {
      return new SingletonDockerResourceWrapper<>(dockerResource);
    }

    /**
     * Set {@link DockerResource} which will be wrapped as a singleton.
     *
     * @param dockerResource Preconfigured {@link DockerResource} which needs to be wrapped
     */
    public Builder<T> setDockerResource(T dockerResource) {
      this.dockerResource = dockerResource;
      return this;
    }
  }
}
