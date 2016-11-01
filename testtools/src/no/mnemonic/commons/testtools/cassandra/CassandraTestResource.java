package no.mnemonic.commons.testtools.cassandra;

import info.archinnov.achilles.embedded.CassandraEmbeddedServer;
import info.archinnov.achilles.embedded.CassandraEmbeddedServerBuilder;
import no.mnemonic.commons.testtools.AvailablePortFinder;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This resource can be used to initialize an embedded Cassandra server in unit tests using JUnit.
 * <p>
 * A CQL start-up script can be provided in order to initialize the Cassandra server by e.g. creating tables.
 * <p>
 * For example:
 * <p>
 * CassandraTestResource.builder()
 *  .setClusterName("Cluster name")
 *  .setKeyspaceName("keyspace")
 *  .setStartupScript("setup.cql")
 *  .build();
 * <p>
 * This will start a Cassandra server with a cluster name, a keyspace and runs a start-up script.
 * A port the server will listen on is selected automatically.
 */
public class CassandraTestResource extends ExternalResource {
  private static final Logger LOG = LoggerFactory.getLogger(CassandraTestResource.class);

  private final String clusterName;
  private final String keyspaceName;
  private final String startupScript;
  private static int port;
  private static CassandraEmbeddedServer server;

  private CassandraTestResource(String clusterName, String keyspaceName, String startupScript) {
    this.clusterName = clusterName;
    this.keyspaceName = keyspaceName;
    this.startupScript = startupScript;
  }

  @Override
  protected void before() throws Throwable {
    synchronized (CassandraTestResource.class) {
      // only initialize server once
      if (server == null) {
        port = AvailablePortFinder.getAvailablePort(12000);
        server = CassandraEmbeddedServerBuilder.builder()
                .cleanDataFilesAtStartup(true)
                .withClusterName(clusterName)
                .withKeyspaceName(keyspaceName)
                .withScript(startupScript)
                .withCQLPort(port)
                .buildServer();
        LOG.info("Cassandra server init success on port " + port);
      }
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String clusterName;
    private String keyspaceName;
    private String startupScript;

    public CassandraTestResource build() {
      return new CassandraTestResource(clusterName, keyspaceName, startupScript);
    }

    public Builder setClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder setKeyspaceName(String keyspaceName) {
      this.keyspaceName = keyspaceName;
      return this;
    }

    public Builder setStartupScript(String startupScript) {
      this.startupScript = startupScript;
      return this;
    }
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getKeyspaceName() {
    return keyspaceName;
  }

  public String getStartupScript() {
    return startupScript;
  }

  public int getPort() {
    return port;
  }

  public CassandraEmbeddedServer getServer() {
    return server;
  }
}
