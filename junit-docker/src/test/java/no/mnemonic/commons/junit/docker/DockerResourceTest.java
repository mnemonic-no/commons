package no.mnemonic.commons.junit.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.NetworkSettings;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.shaded.com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class DockerResourceTest {

  @Mock
  private DockerClient dockerClient;

  private DockerResource resource;

  @Before
  public void setUp() {
    initMocks(this);

    resource = DockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName("busybox")
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
            .setImageName("busybox")
            .setReachabilityTimeout(30)
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingReachabilityTimeout() {
    DockerResource.builder()
            .setImageName("busybox")
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

    verify(dockerClient).ping();
    verify(dockerClient).createContainer(argThat(containerConfig -> {
      assertEquals("busybox", containerConfig.image());
      assertTrue(containerConfig.exposedPorts().contains("8080"));
      assertTrue(containerConfig.hostConfig().portBindings().containsKey("8080"));
      return true;
    }));
    verify(dockerClient).startContainer(eq("containerID"));
    verifyNoMoreInteractions(dockerClient);
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

  private void mockStartContainer() throws Exception {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    // dockerClient.startContainer() returns void, thus, no need to mock it.
  }

  private void mockInspectContainer() throws Exception {
    NetworkSettings networkSettings = mock(NetworkSettings.class);
    ContainerInfo containerInfo = mock(ContainerInfo.class);
    when(networkSettings.ports()).thenReturn(ImmutableMap.<String, List<PortBinding>>builder()
            .put("8080/tcp", list(PortBinding.of("0.0.0.0", "33333")))
            .build());
    when(containerInfo.networkSettings()).thenReturn(networkSettings);
    when(dockerClient.inspectContainer(any())).thenReturn(containerInfo);
  }
}
