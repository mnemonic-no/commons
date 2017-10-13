package no.mnemonic.commons.junit.docker;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ExecCreation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CassandraDockerResourceTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();
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
    expectIllegalStateException("Could not execute 'cqlsh'");
    when(dockerClient.execCreate(any(), any(), (DockerClient.ExecCreateParam[]) any())).thenThrow(DockerException.class);
    resource.before();
  }

  @Test
  public void testIsContainerReachableFailsOnExecStartException() throws Throwable {
    expectIllegalStateException("Could not execute 'cqlsh'");
    when(dockerClient.execCreate(any(), any(), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create("executionID", null));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    resource.before();
  }

  @Test(expected = TimeoutException.class)
  public void testIsContainerReachableWithConnectionError() throws Throwable {
    mockTestReachability("Connection error");
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnCopyToContainerException() throws Throwable {
    expectIllegalStateException("Could not copy files");
    mockTestReachability("Success");
    doThrow(DockerException.class).when(dockerClient).copyToContainer(isA(Path.class), any(), any());
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnExecCreateException() throws Throwable {
    expectIllegalStateException("Could not execute CQL script");
    mockTestReachability("Success");
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/setup.cql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenThrow(DockerException.class);
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnExecStartException() throws Throwable {
    expectIllegalStateException("Could not execute CQL script");
    mockTestReachability("Success");
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/setup.cql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create("executionID", null));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnExecuteScript() throws Throwable {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Evaluation of CQL script");
    mockTestReachability("Success");
    mockExecuteScript("setup.cql", "Failure");
    resource.before();
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockAndExecuteSuccessfulInitialization();

    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-e", "describe cluster"}), (DockerClient.ExecCreateParam[]) any());
    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/setup.cql"}), (DockerClient.ExecCreateParam[]) any());
    verify(dockerClient, times(2)).execStart(any());
    verify(dockerClient, times(2)).copyToContainer(isA(Path.class), any(), eq("/tmp/"));
  }

  @Test
  public void testInitializeSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability("Success");
    resourceWithoutScripts.before();

    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-e", "describe cluster"}), (DockerClient.ExecCreateParam[]) any());
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
    expectIllegalStateException("Could not execute CQL script");
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();
  }

  @Test
  public void testTruncateFailsOnExecStartException() throws Throwable {
    expectIllegalStateException("Could not execute CQL script");
    when(dockerClient.execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create("executionID", null));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();
  }

  @Test
  public void testTruncateFailsOnExecuteScript() throws Throwable {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Evaluation of CQL script");
    mockExecuteScript("truncate.cql", "Failure");
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();
  }

  @Test
  public void testTruncateSuccessful() throws Throwable {
    mockExecuteScript("truncate.cql", "");
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();

    verify(dockerClient).execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), (DockerClient.ExecCreateParam[]) any());
  }

  @Test
  public void testTruncateSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability("Success");
    resourceWithoutScripts.before();
    resourceWithoutScripts.truncate();

    verify(dockerClient, never()).execCreate(any(), eq(new String[]{"cqlsh", "-f", "/tmp/truncate.cql"}), (DockerClient.ExecCreateParam[]) any());
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
