package no.mnemonic.commons.junit.docker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
  public void testNullOrBlank() {
    assertEquals("localhost", DockerTestUtils.extractHost(null));
    assertEquals("localhost", DockerTestUtils.extractHost(""));
    assertEquals("localhost", DockerTestUtils.extractHost(" "));
  }

  @Test
  public void testInvalidFormat() {
    assertThrows(IllegalArgumentException.class, () -> DockerTestUtils.extractHost("invalid"));
    assertThrows(IllegalArgumentException.class, () -> DockerTestUtils.extractHost("tcp://my.host"));
    assertThrows(IllegalArgumentException.class, () -> DockerTestUtils.extractHost("http://my.host:1234"));
  }
}
