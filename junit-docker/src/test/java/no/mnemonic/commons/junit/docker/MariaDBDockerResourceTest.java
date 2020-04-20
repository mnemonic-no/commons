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
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class MariaDBDockerResourceTest {

  private static final String MYSQLD_IS_ALIVE = "mysqld is alive\n";
  private static final String COULD_NOT_EXECUTE_SQL_SCRIPT = "Could not execute SQL script";

  @Mock
  private DockerClient dockerClient;

  private MariaDBDockerResource resource;
  private MariaDBDockerResource resourceWithoutScripts;

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

    resourceWithoutScripts = MariaDBDockerResource.builder()
            .setDockerClientResolver(() -> dockerClient)
            .setImageName("mariadb:10.0")
            .setReachabilityTimeout(1)
            .addApplicationPort(3306)
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingSetupScript() {
    MariaDBDockerResource.builder()
            .setImageName("mariadb:10.0")
            .setSetupScript("notExist.cql")
            .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFailsOnMissingTruncateScript() {
    MariaDBDockerResource.builder()
            .setImageName("mariadb:10.0")
            .setTruncateScript("notExist.cql")
            .build();
  }

  @Test
  public void testIsContainerReachableFailsOnExecCreateException() throws Throwable {
    when(dockerClient.execCreate(any(), any(), any())).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not execute 'mysqladmin'"));
  }

  @Test
  public void testIsContainerReachableFailsOnExecStartException() throws Throwable {
    when(dockerClient.execCreate(any(), any(), any())).thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not execute 'mysqladmin'"));
  }

  @Test(expected = TimeoutException.class)
  public void testIsContainerReachableWithConnectionError() throws Throwable {
    mockTestReachability("Connection error");
    resource.before();
  }

  @Test
  public void testPrepareContainerFailsOnCopyToContainerException() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    doThrow(DockerException.class).when(dockerClient).copyToContainer(isA(Path.class), any(), any());

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Could not copy files"));
  }

  @Test
  public void testPrepareContainerFailsOnExecCreateException() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), any()))
            .thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testPrepareContainerFailsOnExecStartException() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), any()))
            .thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testPrepareContainerFailsOnExecuteScript() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    mockExecuteScript("setup.sql", "Failure");

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.before());
    assertTrue(ex.getMessage().contains("Evaluation of SQL script"));
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockAndExecuteSuccessfulInitialization();

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysqladmin", "-uroot", "-proot", "ping"}), any());
    verify(dockerClient).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), any());
    verify(dockerClient, times(2)).execStart(any());
  }

  @Test
  public void testInitializeSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    resourceWithoutScripts.before();

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysqladmin", "-uroot", "-proot", "ping"}), any());
    verify(dockerClient).execStart(any());
    verify(dockerClient, never()).copyToContainer(isA(Path.class), any(), eq("/tmp/"));
  }

  @Test
  public void testInitializeAddsAdditionalConfig() throws Throwable {
    mockAndExecuteSuccessfulInitialization();

    verify(dockerClient).createContainer(argThat(containerConfig -> {
      assertTrue(containerConfig.hostConfig().tmpfs().containsKey("/var/lib/mysql"));
      assertEquals(0, (int) containerConfig.hostConfig().memorySwappiness());
      return true;
    }));
  }

  @Test
  public void testTruncateFailsOnExecCreateException() throws Throwable {
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), any()))
            .thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.truncate());
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testTruncateFailsOnExecStartException() throws Throwable {
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), any()))
            .thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.truncate());
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testTruncateFailsOnExecuteScript() throws Throwable {
    mockExecuteScript("truncate.sql", "Failure");
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> resource.truncate());
    assertTrue(ex.getMessage().contains("Evaluation of SQL script"));
  }

  @Test
  public void testTruncateSuccessful() throws Throwable {
    mockExecuteScript("truncate.sql", "");
    mockAndExecuteSuccessfulInitialization();
    resource.truncate();

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), any());
  }

  @Test
  public void testTruncateSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    resourceWithoutScripts.before();
    resourceWithoutScripts.truncate();

    verify(dockerClient, never()).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), any());
  }

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
    when(dockerClient.execCreate(any(), eq(command), any())).thenReturn(execCreation(executionID));
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
