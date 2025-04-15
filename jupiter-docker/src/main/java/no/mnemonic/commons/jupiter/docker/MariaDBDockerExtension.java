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
 * MariaDBDockerExtension is a JUnit5 extension which can be used to write integration tests against a MariaDB server
 * executed inside an isolated Docker container. It extends the basic {@link DockerExtension} and makes sure that the
 * container initialization waits until MariaDB is available. It is also possible to initialize MariaDB with a schema
 * and data by providing a SQL start up script. In addition, data stored into MariaDB will be automatically truncated
 * after each test if the extension was initialized with a truncate SQL script.
 * <p>
 * Initialize MariaDBDockerExtension in the following way using {@link RegisterExtension}:
 * <pre>
 * {@code @RegisterExtension
 * public static MariaDBDockerExtension mariadb = MariaDBDockerExtension.builder()
 *   .setImageName("mariadb:10.0")
 *   .setSetupScript("setup.sql")
 *   .setTruncateScript("truncate.sql")
 *   .build();}
 * </pre>
 * This extension will by default expose port 3306.
 * <p>
 * See {@link DockerExtension.Builder} and {@link MariaDBDockerExtension.Builder} for more information on the
 * configuration properties.
 */
public class MariaDBDockerExtension extends DockerExtension {

  private static final Logger LOGGER = Logging.getLogger(MariaDBDockerExtension.class);
  private static final String SUCCESS_MESSAGE = "mysqld is alive\n";

  private final Path setupScript;
  private final Path truncateScript;

  private MariaDBDockerExtension(String imageName,
                                 Set<Integer> applicationPorts,
                                 String exposedPortsRange,
                                 int reachabilityTimeout,
                                 boolean skipReachabilityCheck,
                                 boolean skipPullDockerImage,
                                 Supplier<DockerClient> dockerClientResolver,
                                 Map<String, String> environmentVariables,
                                 String setupScript,
                                 String truncateScript) {
    super(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout, skipReachabilityCheck,
            skipPullDockerImage, dockerClientResolver, environmentVariables);
    // Both parameters are optional.
    this.setupScript = !StringUtils.isBlank(setupScript) ? checkFileExists(setupScript) : null;
    this.truncateScript = !StringUtils.isBlank(truncateScript) ? checkFileExists(truncateScript) : null;
  }

  /**
   * Create builder for MariaDBDockerExtension.
   *
   * @return Builder object
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Truncate data stored inside the database after each test by executing the truncate SQL script.
   *
   * @throws IllegalStateException If SQL script could not be executed
   */
  @Override
  public void afterEach(ExtensionContext context) {
    ObjectUtils.ifNotNullDo(truncateScript, this::executeSqlScript);
  }

  /**
   * Adds MariaDB specific host configuration to default configuration from {@link DockerExtension}.
   *
   * @param config Default configuration as set up by DockerExtension
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
      LOGGER.warning(ex, "Could not execute 'mysqladmin' to test for MariaDB reachability.");
      return false;
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
   * Builder to create {@link MariaDBDockerExtension} which extends {@link DockerExtension.Builder}.
   */
  public static class Builder extends DockerExtension.Builder<Builder> {
    private String setupScript;
    private String truncateScript;

    /**
     * Build a configured MariaDBDockerExtension.
     *
     * @return Configured MariaDBDockerExtension
     */
    @Override
    public MariaDBDockerExtension build() {
      //add 3306 by default
      addApplicationPort(3306);
      addEnvironmentVariable("MYSQL_ROOT_PASSWORD", "root");
      addEnvironmentVariable("MYSQL_ROOT_HOST", "%");
      return new MariaDBDockerExtension(imageName, applicationPorts, exposedPortsRange, reachabilityTimeout,
              skipReachabilityCheck, skipPullDockerImage, dockerClientResolver, environmentVariables, setupScript, truncateScript);
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
