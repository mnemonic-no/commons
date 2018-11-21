package no.mnemonic.commons.junit.docker;

import com.spotify.docker.client.DockerClient;
import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.spotify.docker.client.DockerClient.ExecCreateParam.*;

/**
 * ElasticSearchDockerResource is a JUnit resource which can be used to write integration tests against an ElasticSearch
 * server executed inside an isolated Docker container. It extends the basic {@link DockerResource} and makes sure that
 * the container initialization waits until ElasticSearch is available. Data indexed into ElasticSearch by tests can be
 * truncated by providing the indices to delete when constructing the resource and calling {@link #deleteIndices()}.
 * <p>
 * Initialize ElasticSearchDockerResource in the following way as a {@link org.junit.ClassRule}:
 * <pre>
 * {@code @ClassRule
 *  public static ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
 *     .setImageName("elasticsearch")
 *     .addApplicationPort(9200)
 *     .addApplicationPort(9300)
 *     .addDeleteIndex("foo")
 *     .addDeleteIndex("bar")
 *     .addDeleteIndex("baz")
 *     .build();}
 * </pre>
 * See {@link DockerResource.Builder} and {@link ElasticSearchDockerResource.Builder} for more information on the
 * configuration properties.
 */
public class ElasticSearchDockerResource extends DockerResource {

  private final Set<String> deleteIndices;

  private ElasticSearchDockerResource(String imageName, Set<Integer> applicationPort, String exposedPortsRange,
                                      int reachabilityTimeout, Supplier<DockerClient> dockerClientResolver,
                                      Set<String> deleteIndices, Map<String, String> environmentVariables) {
    super(imageName, applicationPort, exposedPortsRange, reachabilityTimeout, dockerClientResolver,
            environmentVariables);

    // The 'deleteIndices' parameter is optional.
    this.deleteIndices = ObjectUtils.ifNotNull(deleteIndices, Collections::unmodifiableSet, Collections.emptySet());
  }

  /**
   * Truncate data indexed into ElasticSearch by deleting the indices specified when constructing the resource.
   * If no indices were specified <em>all</em> indices will be deleted.
   *
   * @throws IllegalStateException If indices could not be deleted
   */
  public void deleteIndices() {
    // If 'deleteIndices' is empty delete all indices, otherwise only delete the specified indices.
    String indicesToDelete = "_all";
    if (!CollectionUtils.isEmpty(deleteIndices)) {
      indicesToDelete = String.join(",", deleteIndices);
    }

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
      throw new IllegalStateException(String.format("Could not delete indices %s.\n%s", indicesToDelete, output));
    }
  }

  /**
   * Create builder for ElasticSearchDockerResource.
   *
   * @return Builder object
   */
  public static Builder builder() {
    return new Builder();
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
   * Builder to create an ElasticSearchDockerResource which extends {@link DockerResource.Builder}.
   */
  public static class Builder extends DockerResource.Builder<Builder> {
    private Set<String> deleteIndices;

    /**
     * Build a configured ElasticSearchDockerResource.
     *
     * @return Configured ElasticSearchDockerResource
     */
    @Override
    public ElasticSearchDockerResource build() {
      return new ElasticSearchDockerResource(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout,
              dockerClientResolver, deleteIndices, environmentVariables);
    }

    /**
     * Set indices to delete when calling {@link #deleteIndices()}. If no indices were specified and {@link #deleteIndices()}
     * is called, <em>all</em> indices will be deleted.
     *
     * @param deleteIndices Set of indices to delete
     * @return Builder
     */
    public Builder setDeleteIndices(Set<String> deleteIndices) {
      this.deleteIndices = deleteIndices;
      return this;
    }

    /**
     * Add an additional index to delete when calling {@link #deleteIndices()}. If no indices were specified and
     * {@link #deleteIndices()} is called, <em>all</em> indices will be deleted.
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
