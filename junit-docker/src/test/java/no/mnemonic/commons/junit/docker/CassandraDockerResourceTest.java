package no.mnemonic.commons.junit.docker;

import org.junit.Before;
import org.junit.Test;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.LogStream;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerCreation;
import org.mandas.docker.client.messages.ExecCreation;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CassandraDockerResourceTest {

  @Mock
  private DockerClient dockerClient;

  private CassandraDockerResource resource;
  private CassandraDockerResource resourceWithoutScripts;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    mockStartContainer();

    resource = CassandraDockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName("cassandra")
            .setReachabilityTimeout(1)
            .addApplicationPort(9042)
            .setSetupScript("setup.cql")
            .setTruncateScript("truncate.cql")
            .build();

    resourceWithoutScripts = CassandraDockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName("cassandra")
            .setReachabilityTimeout(1)
            .addApplicationPort(9042)
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingSetupScript() {
    CassandraDockerResource.builder()
            .setImageName("cassandra")
            .addApplicationPort(9042)
            .setSetupScript("notExist.cql")
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingTruncateScript() {
    CassandraDockerResource.builder()
            .setImageName("cassandra")
            .addApplicationPort(9042)
            .setTruncateScript("notExist.cql")
            .build();
  }

  @Test
  public void testIsContainerReachableFailsOnExecCreateException() throws Throwable {
    when(dockerClient.execCreate(any(), any(), any())).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not execute 'cqlsh'"));
  }

  @Test
  public void testIsContainerReachableFailsOnExecStartException() throws Throwable {
    when(dockerClient.execCreate(any(), any(), any())).thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not execute 'cqlsh'"));
  }

  @Test(expected = TimeoutException.class)
  public void testIsContainerReachableWithConnectionError() throws Throwable {
    mockTestReachability("Connection error");
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnCopyToContainerException() throws Throwable {
    mockTestReachability("Success");
    doThrow(DockerException.class).when(dockerClient).copyToContainer(isA(Path.class), any(), any());

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not copy files"));
  }

  @Test
  public void testPrepareContainerFailsOnExecCreateException() throws Throwable {
    mockTestReachability("Success");
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/setup.cql"}), any())).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not execute CQL script"));
  }

  @Test
  public void testPrepareContainerFailsOnExecStartException() throws Throwable {
    mockTestReachability("Success");
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/setup.cql"}), any())).thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not execute CQL script"));
  }

  @Test
  public void testPrepareContainerFailsOnExecuteScript() throws Throwable {
    mockTestReachability("Success");
    mockExecuteScript("setup.cql", "Failure");

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Evaluation of CQL script"));
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockAndExecuteSuccessfulInitialization();

    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-e", "describe cluster"}), any(), any(), any());
    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/setup.cql"}), any(), any(), any());
    verify(dockerClient, times(2)).execStart(any());
    verify(dockerClient, times(2)).copyToContainer(isA(Path.class), any(), eq("/tmp/"));
  }

  @Test
  public void testInitializeSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability("Success");
    resourceWithoutScripts.before();

    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-e", "describe cluster"}), any(), any(), any());
    verify(dockerClient).execStart(any());
    verify(dockerClient, never()).copyToContainer(isA(Path.class), any(), eq("/tmp/"));
  }

  @Test
  public void testInitializeAddsAdditionalConfig() throws Throwable {
    mockAndExecuteSuccessfulInitialization();

    verify(dockerClient).createContainer(argThat(containerConfig -> {
      assertTrue(containerConfig.hostConfig().tmpfs().containsKey("/var/lib/cassandra"));
      assertEquals(0, (int) containerConfig.hostConfig().memorySwappiness());
      return true;
    }));
  }

  @Test
  public void testTruncateFailsOnExecCreateException() throws Throwable {
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), any())).thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.truncate());
    assertTrue(ex.getMessage().contains("Could not execute CQL script"));
  }

  @Test
  public void testTruncateFailsOnExecStartException() throws Throwable {
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), any())).thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.truncate());
    assertTrue(ex.getMessage().contains("Could not execute CQL script"));
  }

  @Test
  public void testTruncateFailsOnExecuteScript() throws Throwable {
    mockExecuteScript("truncate.cql", "Failure");
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.truncate());
    assertTrue(ex.getMessage().contains("Evaluation of CQL script"));
  }

  @Test
  public void testTruncateSuccessful() throws Throwable {
    mockExecuteScript("truncate.cql", "");
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();

    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), any(), any(), any());
  }

  @Test
  public void testTruncateSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability("Success");
    resourceWithoutScripts.before();
    resourceWithoutScripts.truncate();

    verify(dockerClient, never()).execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), any());
  }

  private void mockAndExecuteSuccessfulInitialization() throws Throwable {
    mockTestReachability("Success");
    mockExecuteScript("setup.cql", "");
    resource.before();
  }

  private void mockStartContainer() throws Exception {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    // dockerClient.startContainer() returns void, thus, no need to mock it.
  }

  private void mockTestReachability(String output) throws Exception {
    mockExecute(new String[]{"cqlsh", "-e", "describe cluster"}, output);
  }

  private void mockExecuteScript(String name, String output) throws Exception {
    mockExecute(new String[]{"cqlsh", "-f", "/tmp/" + name}, output);
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
