package no.mnemonic.commons.jupiter.docker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
