package no.mnemonic.commons.junit.docker;

import org.junit.Before;
import org.junit.Test;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerCreation;
import org.mandas.docker.client.messages.ContainerInfo;
import org.mandas.docker.client.messages.NetworkSettings;
import org.mandas.docker.client.messages.PortBinding;
import org.mockito.Mock;

import java.util.Collections;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class DockerResourceTest {

  public static final String IMAGE_NAME = "busybox";
  @Mock
  private DockerClient dockerClient;

  private DockerResource resource;

  @Before
  public void setUp() {
    initMocks(this);

    resource = DockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName(IMAGE_NAME)
            .addEnvironmentVariable("key1", "value1")
            .addEnvironmentVariable("key2", "value2")
            .addApplicationPort(8080)
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingImageName() {
    DockerResource.builder()
            .addApplicationPort(8080)
            .setReachabilityTimeout(30)
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingApplicationPort() {
    DockerResource.builder()
            .setImageName(IMAGE_NAME)
            .setReachabilityTimeout(30)
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingReachabilityTimeout() {
    DockerResource.builder()
            .setImageName(IMAGE_NAME)
            .addApplicationPort(8080)
            .setReachabilityTimeout(0)
            .build();
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializeFailsOnPingException() throws Throwable {
    when(dockerClient.ping()).thenThrow(DockerException.class);
    resource.before();
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializeFailsOnPingResult() throws Throwable {
    when(dockerClient.ping()).thenReturn("Failed");
    resource.before();
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializeFailsOnCreateContainerException() throws Throwable {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenThrow(DockerException.class);
    resource.before();
  }

  @Test(expected = IllegalStateException.class)
  public void testInitializeFailsOnStartContainerException() throws Throwable {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    doThrow(DockerException.class).when(dockerClient).startContainer(any());
    resource.before();
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockStartContainer();
    resource.before();

    verify(dockerClient).pull(IMAGE_NAME);
    verify(dockerClient).ping();
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
    resource.before();
    resource.after();

    verify(dockerClient).stopContainer(eq("containerID"), anyInt());
    verify(dockerClient).removeContainer(eq("containerID"));
    verify(dockerClient).close();
  }

  @Test
  public void testTeardownExceptionOnStopContainer() throws Throwable {
    doThrow(DockerException.class).when(dockerClient).stopContainer(any(), anyInt());
    mockStartContainer();
    resource.before();
    resource.after();

    verify(dockerClient).close();
  }

  @Test
  public void testTeardownExceptionOnRemoveContainer() throws Throwable {
    doThrow(DockerException.class).when(dockerClient).removeContainer(any());
    mockStartContainer();
    resource.before();
    resource.after();

    verify(dockerClient).close();
  }

  @Test(expected = IllegalStateException.class)
  public void testGetExposedHostPortFailsOnInspectContainerException() throws Throwable {
    mockStartContainer();
    when(dockerClient.inspectContainer(any())).thenThrow(DockerException.class);
    resource.before();
    resource.getExposedHostPort(8080);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetExposedHostPortFailsOnHostPortNotFound() throws Throwable {
    mockStartContainer();
    mockInspectContainer();
    resource.before();
    resource.getExposedHostPort(8888);
  }

  @Test
  public void testGetExposedHostPortSuccessful() throws Throwable {
    mockStartContainer();
    mockInspectContainer();
    resource.before();
    assertEquals(33333, resource.getExposedHostPort(8080));
  }

  @Test
  public void testSkipPullNewImage() throws Throwable {
    resource = DockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName(IMAGE_NAME)
            .addApplicationPort(8080)
            .setSkipPullDockerImage(true)
            .build();
    mockStartContainer();

    resource.before();
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
    when(networkSettings.ports()).thenReturn(Collections.singletonMap("8080/tcp", list(PortBinding.of("0.0.0.0", "33333"))));
    when(containerInfo.networkSettings()).thenReturn(networkSettings);
    when(dockerClient.inspectContainer(any())).thenReturn(containerInfo);
  }
}
