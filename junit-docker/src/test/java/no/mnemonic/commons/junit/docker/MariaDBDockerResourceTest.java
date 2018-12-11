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

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MariaDBDockerResourceTest {

  private static final String MYSQLD_IS_ALIVE = "mysqld is alive\n";
  private static final String COULD_NOT_EXECUTE_SQL_SCRIPT = "Could not execute SQL script";

  @Rule
  public ExpectedException exception = ExpectedException.none();
  @Mock
  private DockerClient dockerClient;

  private MariaDBDockerResource resource;

  @Before
  public void setUp() throws Exception {
    initMocks(this);
    mockStartContainer();

    resource = MariaDBDockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName("mariadb:10.0")
            .setSetupScript("setup.sql")
            .setTruncateScript("truncate.sql")
            .setReachabilityTimeout(1)
            .addApplicationPort(3306)
            .build();
  }

  @Test(expected = TimeoutException.class)
  public void testIsContainerReachableWithConnectionError() throws Throwable {
    mockTestReachability("Connection error");
    resource.before();
  }


  @Test
  public void testPrepareContainerFailsOnExecCreateException() throws Throwable {
    expectIllegalStateException(COULD_NOT_EXECUTE_SQL_SCRIPT);
    mockTestReachability(MYSQLD_IS_ALIVE);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenThrow(DockerException.class);
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnExecStartException() throws Throwable {
    expectIllegalStateException(COULD_NOT_EXECUTE_SQL_SCRIPT);
    mockTestReachability(MYSQLD_IS_ALIVE);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create("executionID", null));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnExecuteScript() throws Throwable {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Evaluation of SQL script");
    mockTestReachability(MYSQLD_IS_ALIVE);
    mockExecuteScript("setup.sql", "Failure");
    resource.before();
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockAndExecuteSuccessfulInitialization();

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysqladmin", "-uroot", "-proot", "ping"}), (DockerClient.ExecCreateParam[]) any());
    verify(dockerClient).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), (DockerClient.ExecCreateParam[]) any());
    verify(dockerClient, times(2)).execStart(any());
  }

  @Test
  public void testTruncateFailsOnExecCreateException() throws Throwable {
    expectIllegalStateException(COULD_NOT_EXECUTE_SQL_SCRIPT);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();
  }

  @Test
  public void testTruncateFailsOnExecStartException() throws Throwable {
    expectIllegalStateException(COULD_NOT_EXECUTE_SQL_SCRIPT);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), (DockerClient.ExecCreateParam[]) any()))
            .thenReturn(ExecCreation.create("executionID", null));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();
  }

  @Test
  public void testTruncateFailsOnExecuteScript() throws Throwable {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Evaluation of SQL script");
    mockExecuteScript("truncate.sql", "Failure");
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();
  }

  @Test
  public void testTruncateSuccessful() throws Throwable {
    mockExecuteScript("truncate.sql", "");
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), (DockerClient.ExecCreateParam[]) any());
  }

  //helper methods

  private void mockAndExecuteSuccessfulInitialization() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    mockExecuteScript("setup.sql", "");
    resource.before();
  }

  private void mockStartContainer() throws Exception {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    // dockerClient.startContainer() returns void, thus, no need to mock it.
  }

  private void mockTestReachability(String output) throws Exception {
    mockExecute(new String[]{"mysqladmin", "-uroot", "-proot", "ping"}, output);
  }

  private void mockExecuteScript(String name, String output) throws Exception {
    mockExecute(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/" + name}, output);
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
