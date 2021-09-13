package no.mnemonic.commons.junit.docker;

import no.mnemonic.commons.utilities.ObjectUtils;
import no.mnemonic.commons.utilities.StringUtils;
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
 * MariaDBDockerResource is a JUnit resource which can be used to write integration tests against a MariaDB
 * server executed inside an isolated Docker container. It extends the basic {@link DockerResource} and makes sure that
 * the container initialization waits until MariaDB is available.
 * <p>
 * Initialize MariaDBDockerResource in the following way as a {@link org.junit.ClassRule}:
 * <pre>
 * {@code @ClassRule
 *  public static MariaDBDockerResource elastic = MariaDBDockerResource.builder()
 *     .setImageName("mariadb:10.0")
 *     .build(); }
 * </pre>
 * This resource will by default expose port 3306.
 * See {@link DockerResource.Builder} and {@link MariaDBDockerResource.Builder} for more information on the
 * configuration properties.
 */
public class MariaDBDockerResource extends DockerResource {

  private static final String SUCCESS_MESSAGE = "mysqld is alive\n";

  private final Path setupScript;
  private final Path truncateScript;

  private MariaDBDockerResource(String imageName,
                                Set<Integer> applicationPorts,
                                String exposedPortsRange,
                                int reachabilityTimeout,
                                boolean skipReachabilityCheck,
                                Supplier<DockerClient> dockerClientResolver,
                                Map<String, String> environmentVariables,
                                String setupScript,
                                String truncateScript) {
    super(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout, skipReachabilityCheck,
            dockerClientResolver, environmentVariables);
    // Both parameters are optional.
    this.setupScript = !StringUtils.isBlank(setupScript) ? checkFileExists(setupScript) : null;
    this.truncateScript = !StringUtils.isBlank(truncateScript) ? checkFileExists(truncateScript) : null;
  }

  /**
   * Truncate data stored inside the database by executing the truncate SQL script.
   *
   * @throws IllegalStateException If SQL script could not be executed
   */
  public void truncate() {
    ObjectUtils.ifNotNullDo(truncateScript, this::executeSqlScript);
  }

  /**
   * Create builder for MariaDBDockerResource.
   *
   * @return Builder object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Adds MariaDB specific host configuration to default configuration from {@link DockerResource}.
   *
   * @param config Default configuration as set up by DockerResource
   * @return Modified host configuration
   */
  @Override
  protected HostConfig additionalHostConfig(HostConfig config) {
    return config.toBuilder()
            // Write data to tmpfs.
            .tmpfs(map(T("/var/lib/mysql", "")))
            // Deactivate swap.
            .memorySwappiness(0)
            .build();
  }

  /**
   * Verifies that MariaDB is reachable by issuing a simple mysqladmin command inside the MariaDB Docker container.
   *
   * @return True if mysqladmin command returns successfully
   * @throws IllegalStateException If mysqladmin command could not be executed
   */
  @Override
  protected boolean isContainerReachable() {
    try {
      // Workaround for https://github.com/spotify/docker-client/issues/513: also attach stdin.
      String[] cmd = {"mysqladmin", "-uroot", "-proot", "ping"};
      String id = getDockerClient().execCreate(getContainerID(), cmd, attachStdout(), attachStderr(), attachStdin()).id();
      String output = getDockerClient().execStart(id).readFully();

      return SUCCESS_MESSAGE.equals(output);
    } catch (Exception ex) {
      throw new IllegalStateException("Could not execute 'mysqladmin' to test for MariaDB reachability.", ex);
    }
  }

  /**
   * Initializes MariaDB by executing the set up SQL script.
   *
   * @throws IllegalStateException If SQL script could not be executed
   */
  @Override
  protected void prepareContainer() {
    copyFilesToContainer();
    ObjectUtils.ifNotNullDo(setupScript, this::executeSqlScript);
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

  private void executeSqlScript(Path script) {
    String output;

    try {
      // Workaround for https://github.com/spotify/docker-client/issues/513: also attach stdin.
      String[] cmd = {"mysql", "-uroot", "-proot", "-e", "source /tmp/" + script.getFileName()};
      String id = getDockerClient().execCreate(getContainerID(), cmd, attachStdout(), attachStderr(), attachStdin()).id();
      output = getDockerClient().execStart(id).readFully();
    } catch (Exception ex) {
      throw new IllegalStateException(String.format("Could not execute SQL script %s.", script.getFileName()), ex);
    }

    if (!StringUtils.isBlank(output)) {
      throw new IllegalStateException(String.format("Evaluation of SQL script %s failed.%n%s", script.getFileName(), output));
    }
  }

  /**
   * Builder to create {@link MariaDBDockerResource} which extends {@link DockerResource.Builder}
   */
  public static class Builder extends DockerResource.Builder<Builder> {
    private String setupScript;
    private String truncateScript;

    /**
     * Build a configured MariaDBDockerResource.
     *
     * @return Configured MariaDBDockerResource
     */
    @Override
    public MariaDBDockerResource build() {
      //add 3306 by default
      addApplicationPort(3306);
      addEnvironmentVariable("MYSQL_ROOT_PASSWORD", "root");
      addEnvironmentVariable("MYSQL_ROOT_HOST", "%");
      return new MariaDBDockerResource(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout,
              skipReachabilityCheck, dockerClientResolver, environmentVariables, setupScript, truncateScript);
    }

    /**
     * Set file name of SQL start up script. The file needs to be available on the classpath usually from the test
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
     * Set file name of SQL truncate script. The file needs to be available on the classpath usually from the test
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
