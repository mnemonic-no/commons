package no.mnemonic.commons.jupiter.docker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerCreation;
import org.mandas.docker.client.messages.ContainerInfo;
import org.mandas.docker.client.messages.NetworkSettings;
import org.mandas.docker.client.messages.PortBinding;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DockerExtensionTest {

  public static final String IMAGE_NAME = "busybox";
  @Mock
  private DockerClient dockerClient;

  private DockerExtension extension;

  @BeforeEach
  void setUp() {
    extension = DockerExtension.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName(IMAGE_NAME)
            .addEnvironmentVariable("key1", "value1")
            .addEnvironmentVariable("key2", "value2")
            .addApplicationPort(8080)
            .build();
  }

  @Test
  public void testBuildFailsOnMissingImageName() {
    assertThrows(IllegalArgumentException.class, () -> DockerExtension.builder()
            .addApplicationPort(8080)
            .setReachabilityTimeout(30)
            .build());
  }

  @Test
  public void testBuildFailsOnMissingApplicationPort() {
    assertThrows(IllegalArgumentException.class, () -> DockerExtension.builder()
            .setImageName(IMAGE_NAME)
            .setReachabilityTimeout(30)
            .build());
  }

  @Test
  public void testBuildFailsOnMissingReachabilityTimeout() {
    assertThrows(IllegalArgumentException.class, () -> DockerExtension.builder()
            .setImageName(IMAGE_NAME)
            .addApplicationPort(8080)
            .setReachabilityTimeout(0)
            .build());
  }

  @Test
  public void testInitializeFailsOnPingException() throws Throwable {
    when(dockerClient.ping()).thenThrow(DockerException.class);
    assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
  }

  @Test
  public void testInitializeFailsOnPingResult() throws Throwable {
    when(dockerClient.ping()).thenReturn("Failed");
    assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
  }

  @Test
  public void testInitializeFailsOnCreateContainerException() throws Throwable {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenThrow(DockerException.class);
    assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
  }

  @Test
  public void testInitializeFailsOnStartContainerException() throws Throwable {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    doThrow(DockerException.class).when(dockerClient).startContainer(any());
    assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockStartContainer();
    extension.beforeAll(null);

    verify(dockerClient).ping();
    verify(dockerClient).pull(IMAGE_NAME);
    verify(dockerClient).createContainer(argThat(containerConfig -> {
      assertEquals(IMAGE_NAME, containerConfig.image());
      assertEquals(2, containerConfig.env().size());
      assertEquals("key1=value1", containerConfig.env().get(0));
      assertEquals("key2=value2", containerConfig.env().get(1));
      assertTrue(containerConfig.exposedPorts().contains("8080"));
      assertTrue(containerConfig.hostConfig().portBindings().containsKey("8080"));
      return true;
    }));
    verify(dockerClient).startContainer(eq("containerID"));
    verifyNoMoreInteractions(dockerClient);
  }

  @Test
  public void testTeardownSuccessful() throws Throwable {
    mockStartContainer();
    extension.beforeAll(null);
    extension.afterAll(null);

    verify(dockerClient).stopContainer(eq("containerID"), anyInt());
    verify(dockerClient).removeContainer(eq("containerID"));
    verify(dockerClient).close();
  }

  @Test
  public void testTeardownExceptionOnStopContainer() throws Throwable {
    doThrow(DockerException.class).when(dockerClient).stopContainer(any(), anyInt());
    mockStartContainer();
    extension.beforeAll(null);
    extension.afterAll(null);

    verify(dockerClient).close();
  }

  @Test
  public void testTeardownExceptionOnRemoveContainer() throws Throwable {
    doThrow(DockerException.class).when(dockerClient).removeContainer(any());
    mockStartContainer();
    extension.beforeAll(null);
    extension.afterAll(null);

    verify(dockerClient).close();
  }

  @Test
  public void testGetExposedHostPortFailsOnInspectContainerException() throws Throwable {
    mockStartContainer();
    when(dockerClient.inspectContainer(any())).thenThrow(DockerException.class);
    extension.beforeAll(null);
    assertThrows(IllegalStateException.class, () -> extension.getExposedHostPort(8080));
  }

  @Test
  public void testGetExposedHostPortFailsOnHostPortNotFound() throws Throwable {
    mockStartContainer();
    mockInspectContainer();
    extension.beforeAll(null);
    assertThrows(IllegalStateException.class, () -> extension.getExposedHostPort(8888));
  }

  @Test
  public void testGetExposedHostPortSuccessful() throws Throwable {
    mockStartContainer();
    mockInspectContainer();
    extension.beforeAll(null);
    assertEquals(33333, extension.getExposedHostPort(8080));
  }

  @Test
  public void testSkipPullNewImage() throws Throwable {
    extension = DockerExtension.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName(IMAGE_NAME)
            .addApplicationPort(8080)
            .setSkipPullDockerImage(true)
            .build();
    mockStartContainer();

    extension.beforeAll(null);
    verify(dockerClient, never()).pull(IMAGE_NAME);

  }

  private void mockStartContainer() throws Exception {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    // dockerClient.startContainer() returns void, thus, no need to mock it.
  }

  private void mockInspectContainer() throws Exception {
    NetworkSettings networkSettings = mock(NetworkSettings.class);
    ContainerInfo containerInfo = mock(ContainerInfo.class);
    when(networkSettings.ports()).thenReturn(singletonMap("8080/tcp", singletonList(PortBinding.of("0.0.0.0", "33333"))));
    when(containerInfo.networkSettings()).thenReturn(networkSettings);
    when(dockerClient.inspectContainer(any())).thenReturn(containerInfo);
  }
}
