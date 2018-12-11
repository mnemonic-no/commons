package no.mnemonic.commons.junit.docker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DockerTestUtilsTest {

  @Test
  public void testExtractTcpHost() {
    assertEquals("my.host", DockerTestUtils.extractHost("tcp://my.host:1234"));
  }

  @Test
  public void testExtractLocalHost() {
    assertEquals("localhost", DockerTestUtils.extractHost("unix:///var/run/docker.sock"));
  }

  @Test
  public void testNull() {
    assertEquals("localhost", DockerTestUtils.extractHost(null));
  }
}
