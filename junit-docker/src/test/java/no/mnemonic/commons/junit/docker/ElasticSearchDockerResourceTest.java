package no.mnemonic.commons.junit.docker;

import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.LogStream;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerCreation;
import org.mandas.docker.client.messages.ExecCreation;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ElasticSearchDockerResourceTest {

  private static final String[] REACHABILITY_COMMAND = {"curl", "--silent", "--show-error", "-XGET", "localhost:9200/_cat/health"};
  private static final String[] DELETE_ALL_INDICES_COMMAND = {"curl", "--silent", "--show-error", "-XDELETE", "localhost:9200/_all"};
  private static final String REACHABILITY_SUCCESSFUL_OUTPUT = "1510840808 14:00:08 docker-cluster green 1 1 0 0 0 0 0 0 - 100.0%";

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
    when(dockerClient.execCreate(any(), any(), any())).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("ElasticSearch reachability"));
  }

  @Test
  public void testIsContainerReachableFailsOnExecStartException() throws Throwable {
    when(dockerClient.execCreate(any(), any(), any())).thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("ElasticSearch reachability"));
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

    verify(dockerClient).execCreate(any(), eq(REACHABILITY_COMMAND), any(), any(), any());
    verify(dockerClient).execStart(any());
  }

  @Test
  public void testDeleteIndicesFailsOnExecCreateException() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    when(dockerClient.execCreate(any(), eq(DELETE_ALL_INDICES_COMMAND), any())).thenThrow(DockerException.class);

    resource.before();
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.deleteIndices());
    assertTrue(ex.getMessage().contains("Could not execute 'curl' to delete indices"));
  }

  @Test
  public void testDeleteIndicesFailsOnExecStartException() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    when(dockerClient.execCreate(any(), eq(DELETE_ALL_INDICES_COMMAND), any())).thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);

    resource.before();
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.deleteIndices());
    assertTrue(ex.getMessage().contains("Could not execute 'curl' to delete indices"));
  }

  @Test
  public void testDeleteIndicesFailsOnExecuteCommand() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    mockExecute(DELETE_ALL_INDICES_COMMAND, "{\"error\":true}");

    resource.before();
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.deleteIndices());
    assertTrue(ex.getMessage().contains("Could not delete indices"));
  }

  @Test
  public void testDeleteIndicesAllSuccessful() throws Throwable {
    mockExecute(REACHABILITY_COMMAND, REACHABILITY_SUCCESSFUL_OUTPUT);
    mockExecute(DELETE_ALL_INDICES_COMMAND, "{\"acknowledged\":true}");
    resource.before();
    resource.deleteIndices();

    verify(dockerClient).execCreate(any(), eq(DELETE_ALL_INDICES_COMMAND), any(), any(), any());
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

    verify(dockerClient).execCreate(any(), eq(cmd), any(), any(), any());
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
    when(dockerClient.execCreate(any(), eq(command), any(), any(), any())).thenReturn(execCreation(executionID));
    when(dockerClient.execStart(executionID)).thenReturn(logStream);
  }

  private ExecCreation execCreation(String executionID) {
    return new ExecCreation() {
      @Override
      public String id() {
        return executionID;
      }

      @Override
      public List<String> warnings() {
        return Collections.emptyList();
      }
    };
  }
}
