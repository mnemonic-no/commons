package no.mnemonic.commons.junit.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ExecCreation;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticSearchDockerResourceTest {

  private static final String[] REACHABILITY_COMMAND = {"curl", "--silent", "--show-error", "-XGET", "localhost:9200/_cat/health"};
  private static final String[] DELETE_ALL_INDICES_COMMAND = {"curl", "--silent", "--show-error", "-XDELETE", "localhost:9200/_all"};
  private static final String REACHABILITY_SUCCESSFUL_OUTPUT = "1510840808 14:00:08 docker-cluster green 1 1 0 0 0 0 0 0 - 100.0%";

  @Rule
  public ExpectedException exception = ExpectedException.none();
  @Mock
  private DockerClient dockerClient;

  private ElasticSearchDockerResource resource;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    mockStartContainer();

    resource = ElasticSearchDockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName("elasticsearch")
            .setReachabilityTimeout(1)
            .addApplicationPort(9200)
            .build();
  }

  @Test
  public void testIsContainerReachableFailsOnExecCreateException() throws Throwable {
    expectIllegalStateException("ElasticSearch reachability");
    when(dockerClient.execCreate(any(), any(), (DockerClient.ExecCreateParam[]) any())).thenThrow(DockerException.class);
    resource.before();
  }

  @Test
  public void testIsContainerReachableFailsOnExecStartException() throws Throwable {
    expectIllegalStateException("ElasticSearch reachability");
    when(dockerClient.execCreate(any(), any(), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create("executionID", null));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    resource.before();
  }

  @Test(expected = TimeoutException.class)
  public void testIsContainerReachableWithConnectionRefused() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, "Connection refused");
    resource.before();
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    resource.before();

    verify(dockerClient).execCreate(any(), eq(REACHABILITY_COMMAND), (DockerClient.ExecCreateParam[]) any());
    verify(dockerClient).execStart(any());
  }

  @Test
  public void testInitializeAddsAdditionalConfig() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    resource.before();

    verify(dockerClient).createContainer(argThat(containerConfig -> {
      assertTrue(containerConfig.env().contains("xpack.security.enabled=false"));
      assertTrue(containerConfig.env().contains("xpack.monitoring.enabled=false"));
      assertTrue(containerConfig.env().contains("xpack.ml.enabled=false"));
      assertTrue(containerConfig.env().contains("xpack.graph.enabled=false"));
      assertTrue(containerConfig.env().contains("xpack.watcher.enabled=false"));
      return true;
    }));
  }

  @Test
  public void testDeleteIndicesFailsOnExecCreateException() throws Throwable {
    expectIllegalStateException("Could not execute 'curl' to delete indices");
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    when(dockerClient.execCreate(any(), eq(DELETE_ALL_INDICES_COMMAND), (DockerClient.ExecCreateParam[]) any()))
            .thenThrow(DockerException.class);
    resource.before();
    resource.deleteIndices();
  }

  @Test
  public void testDeleteIndicesFailsOnExecStartException() throws Throwable {
    expectIllegalStateException("Could not execute 'curl' to delete indices");
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    when(dockerClient.execCreate(any(), eq(DELETE_ALL_INDICES_COMMAND), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create("executionID", null));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    resource.before();
    resource.deleteIndices();
  }

  @Test
  public void testDeleteIndicesFailsOnExecuteCommand() throws Throwable {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Could not delete indices");
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    mockExecute(DELETE_ALL_INDICES_COMMAND, "{\"error\":true}");
    resource.before();
    resource.deleteIndices();
  }

  @Test
  public void testDeleteIndicesAllSuccessful() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    mockExecute(DELETE_ALL_INDICES_COMMAND, "{\"acknowledged\":true}");
    resource.before();
    resource.deleteIndices();

    verify(dockerClient).execCreate(any(), eq(DELETE_ALL_INDICES_COMMAND), (DockerClient.ExecCreateParam[]) any());
  }

  @Test
  public void testDeleteIndicesSpecificSuccessful() throws Throwable {
    Set<String> indicesToDelete = SetUtils.set("foo", "bar", "baz");
    String[] cmd = {"curl", "--silent", "--show-error", "-XDELETE", "localhost:9200/" + String.join(",", indicesToDelete)};
    ElasticSearchDockerResource elastic = ElasticSearchDockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName("elasticsearch")
            .addApplicationPort(9200)
            .setDeleteIndices(indicesToDelete)
            .build();

    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    mockExecute(cmd, "{\"acknowledged\":true}");
    elastic.before();
    elastic.deleteIndices();

    verify(dockerClient).execCreate(any(), eq(cmd), (DockerClient.ExecCreateParam[]) any());
  }

  private void mockStartContainer() throws Exception {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    // dockerClient.startContainer() returns void, thus, no need to mock it.
  }

  private void mockExecute(String[] command, String output) throws Exception {
    String executionID = UUID.randomUUID().toString();
    LogStream logStream = mock(LogStream.class);
    when(logStream.readFully()).thenReturn(output);
    when(dockerClient.execCreate(any(), eq(command), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create(executionID, null));
    when(dockerClient.execStart(executionID)).thenReturn(logStream);
  }

  private void expectIllegalStateException(String phrase) {
    // Make sure that correct IllegalStateException is thrown and that wrong/missing mocking will be caught.
    exception.expect(IllegalStateException.class);
    exception.expectCause(instanceOf(DockerException.class));
    exception.expectMessage(phrase);
  }
}
