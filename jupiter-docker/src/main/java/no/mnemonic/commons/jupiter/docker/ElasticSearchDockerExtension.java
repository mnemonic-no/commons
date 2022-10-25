package no.mnemonic.commons.jupiter.docker;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mandas.docker.client.DockerClient;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.mandas.docker.client.DockerClient.ExecCreateParam.*;

/**
 * ElasticSearchDockerExtension is a JUnit5 extension which can be used to write integration tests against an ElasticSearch
 * server executed inside an isolated Docker container. It extends the basic {@link DockerExtension} and makes sure that
 * the container initialization waits until ElasticSearch is available. In addition, data indexed into ElasticSearch will
 * be automatically truncated after each test if the extension was initialized with the corresponding parameter.
 * <p>
 * Initialize ElasticSearchDockerExtension in the following way using {@link RegisterExtension}:
 * <pre>
 * {@code @RegisterExtension
 * public static ElasticSearchDockerExtension elastic = ElasticSearchDockerExtension.builder()
 *   .setImageName("elasticsearch")
 *   .addApplicationPort(9200)
 *   .addApplicationPort(9300)
 *   .addDeleteIndex("foo")
 *   .addDeleteIndex("bar")
 *   .addDeleteIndex("baz")
 *   .build();}
 * </pre>
 * See {@link DockerExtension.Builder} and {@link ElasticSearchDockerExtension.Builder} for more information on the
 * configuration properties.
 */
public class ElasticSearchDockerExtension extends DockerExtension {

  private final Set<String> deleteIndices;

  private ElasticSearchDockerExtension(String imageName,
                                       Set<Integer> applicationPort,
                                       String exposedPortsRange,
                                       int reachabilityTimeout,
                                       boolean skipReachabilityCheck,
                                       boolean skipPullDockerImage,
                                       Supplier<DockerClient> dockerClientResolver,
                                       Set<String> deleteIndices,
                                       Map<String, String> environmentVariables) {
    super(imageName, applicationPort, exposedPortsRange, reachabilityTimeout, skipReachabilityCheck,
            skipPullDockerImage, dockerClientResolver, environmentVariables);

    // The 'deleteIndices' parameter is optional.
    this.deleteIndices = ObjectUtils.ifNotNull(deleteIndices, Collections::unmodifiableSet, Collections.emptySet());
  }

  /**
   * Create builder for ElasticSearchDockerExtension.
   *
   * @return Builder object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Truncate data indexed into ElasticSearch after each test by deleting the indices specified when constructing the extension.
   *
   * @throws IllegalStateException If indices could not be deleted
   */
  @Override
  public void afterEach(ExtensionContext context) {
    if (CollectionUtils.isEmpty(deleteIndices)) return;

    String indicesToDelete = String.join(",", deleteIndices);

    String output;
    try {
      // Use curl and the delete index API to delete the specified indices.
      // Workaround for https://github.com/spotify/docker-client/issues/513: also attach stdin.
      String[] cmd = {"curl", "--silent", "--show-error", "-XDELETE", "localhost:9200/" + indicesToDelete};
      String id = getDockerClient().execCreate(getContainerID(), cmd, attachStdout(), attachStderr(), attachStdin()).id();
      output = getDockerClient().execStart(id).readFully();
    } catch (Exception ex) {
      throw new IllegalStateException("Could not execute 'curl' to delete indices.", ex);
    }

    // Verify that indices were deleted successfully.
    if (StringUtils.isBlank(output) || !output.contains("{\"acknowledged\":true}")) {
      throw new IllegalStateException(String.format("Could not delete indices %s.%n%s", indicesToDelete, output));
    }
  }

  /**
   * Verifies that ElasticSearch is reachable by querying the cluster status from inside the Docker container.
   *
   * @return True if cluster status could be queried successfully
   * @throws IllegalStateException If cluster status could not be queried
   */
  @Override
  protected boolean isContainerReachable() {
    try {
      // Use curl to query health status of the ElasticSearch cluster to test for reachability.
      // Workaround for https://github.com/spotify/docker-client/issues/513: also attach stdin.
      String[] cmd = {"curl", "--silent", "--show-error", "-XGET", "localhost:9200/_cat/health"};
      String id = getDockerClient().execCreate(getContainerID(), cmd, attachStdout(), attachStderr(), attachStdin()).id();
      String output = getDockerClient().execStart(id).readFully();
      // Check if ElasticSearch is available by inspecting the output.
      return isClusterAvailable(output);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not execute 'curl' to test for ElasticSearch reachability.", ex);
    }
  }

  private boolean isClusterAvailable(String output) {
    // If the output contains the phrase "Connection refused" curl failed to connect to ElasticSearch.
    if (StringUtils.isBlank(output) || output.contains("Connection refused")) {
      return false;
    }

    // Parse output and check status field (the 4th field in the output).
    String[] split = output.split(" ");
    return split.length >= 4 && split[3].equals("green");
  }

  /**
   * Builder to create an ElasticSearchDockerExtension which extends {@link DockerExtension.Builder}.
   */
  public static class Builder extends DockerExtension.Builder<Builder> {
    private Set<String> deleteIndices;

    /**
     * Build a configured ElasticSearchDockerExtension.
     *
     * @return Configured ElasticSearchDockerExtension
     */
    @Override
    public ElasticSearchDockerExtension build() {
      return new ElasticSearchDockerExtension(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout,
              skipReachabilityCheck, skipPullDockerImage, dockerClientResolver, deleteIndices, environmentVariables);
    }

    /**
     * Set indices to delete after each test. Specify '_all' to delete <em>all</em> indices.
     *
     * @param deleteIndices Set of indices to delete
     * @return Builder
     */
    public Builder setDeleteIndices(Set<String> deleteIndices) {
      this.deleteIndices = deleteIndices;
      return this;
    }

    /**
     * Add an additional index to delete after each test. Specify '_all' to delete <em>all</em> indices.
     *
     * @param deleteIndex Index to delete
     * @return Builder
     */
    public Builder addDeleteIndex(String deleteIndex) {
      this.deleteIndices = SetUtils.addToSet(this.deleteIndices, deleteIndex);
      return this;
    }
  }
}
