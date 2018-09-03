package no.mnemonic.commons.testtools.cassandra;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.rules.ExternalResource;

import java.util.Set;

/**
 * This rule can be used to truncate Cassandra tables before/after a unit test using JUnit.
 * <p>
 * It can be used to clean-up Cassandra tables between two successive tests.
 * <p>
 * For example:
 * <p>
 * CassandraTruncateRule.builder()
 *  .setKeyspace("keyspace)
 *  .setSession(getCassandraSession())
 *  .setTruncateAfter(true)
 *  .addTable("table")
 *  .build();
 * <p>
 * This will truncate all specified Cassandra tables before/after a test is executed.
 * A Cassandra session needs to be provided, e.g. from a CassandraTestResource.
 *
 * @deprecated Use CassandraDockerResource from junit-docker module instead. It provides better isolation between the
 * Cassandra Server and Client as they are not executed in the same JVM. This class will be removed in the future.
 */
@Deprecated
public class CassandraTruncateRule extends ExternalResource {
  private final String keyspace;
  private final Set<String> tables;
  private final Session session;
  private final boolean truncateAfter;

  private CassandraTruncateRule(String keyspace, Set<String> tables, Session session, boolean truncateAfter) {
    this.keyspace = keyspace;
    this.tables = tables;
    this.session = session;
    this.truncateAfter = truncateAfter;
  }

  @Override
  protected void before() throws Throwable {
    if (!truncateAfter) {
      truncate();
    }
  }

  @Override
  protected void after() {
    if (truncateAfter) {
      truncate();
    }
  }

  private void truncate() {
    for (String table : tables) {
      session.execute(new SimpleStatement(QueryBuilder.truncate(keyspace, table).getQueryString())
              .setReadTimeoutMillis(30_000));
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String keyspace;
    private Set<String> tables;
    private Session session;
    private boolean truncateAfter = true; // default truncate tables after test

    public CassandraTruncateRule build() {
      return new CassandraTruncateRule(keyspace, tables, session, truncateAfter);
    }

    public Builder setKeyspace(String keyspace) {
      this.keyspace = keyspace;
      return this;
    }

    public Builder setTables(Set<String> tables) {
      this.tables = SetUtils.set(tables);
      return this;
    }

    public Builder addTable(String table) {
      this.tables = SetUtils.addToSet(this.tables, table);
      return this;
    }

    public Builder setSession(Session session) {
      this.session = session;
      return this;
    }

    public Builder setTruncateAfter(boolean truncateAfter) {
      this.truncateAfter = truncateAfter;
      return this;
    }
  }
}
