package no.mnemonic.commons.jupiter.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mandas.docker.client.DockerClient;
import org.mandas.docker.client.LogStream;
import org.mandas.docker.client.exceptions.DockerException;
import org.mandas.docker.client.messages.ContainerCreation;
import org.mandas.docker.client.messages.ExecCreation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MariaDBDockerExtensionTest {

  private static final String MYSQLD_IS_ALIVE = "mysqld is alive\n";
  private static final String COULD_NOT_EXECUTE_SQL_SCRIPT = "Could not execute SQL script";

  @Mock
  private DockerClient dockerClient;

  private final MariaDBDockerExtension extension = MariaDBDockerExtension.builder()
          .setDockerClientResolver(() -> dockerClient)
          .setImageName("mariadb:10.0")
          .setSetupScript("setup.sql")
          .setTruncateScript("truncate.sql")
          .setReachabilityTimeout(1)
          .addApplicationPort(3306)
          .build();
  private final MariaDBDockerExtension extensionWithoutScripts = MariaDBDockerExtension.builder()
          .setDockerClientResolver(() -> dockerClient)
          .setImageName("mariadb:10.0")
          .setReachabilityTimeout(1)
          .addApplicationPort(3306)
          .build();

  @Test
  public void testBuildFailsOnMissingSetupScript() {
    assertThrows(IllegalArgumentException.class, () -> MariaDBDockerExtension.builder()
            .setImageName("mariadb:10.0")
            .setSetupScript("notExist.cql")
            .build());
  }

  @Test
  public void testBuildFailsOnMissingTruncateScript() {
    assertThrows(IllegalArgumentException.class, () -> MariaDBDockerExtension.builder()
            .setImageName("mariadb:10.0")
            .setTruncateScript("notExist.cql")
            .build());
  }

  @Test
  public void testIsContainerReachableFailsOnExecCreateException() throws Throwable {
    mockStartContainer();
    when(dockerClient.execCreate(any(), any(), any())).thenThrow(DockerException.class);
    assertThrows(TimeoutException.class, () -> extension.beforeAll(null));
  }

  @Test
  public void testIsContainerReachableFailsOnExecStartException() throws Throwable {
    mockStartContainer();
    when(dockerClient.execCreate(any(), any(), any())).thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    assertThrows(TimeoutException.class, () -> extension.beforeAll(null));
  }

  @Test
  public void testIsContainerReachableWithConnectionError() throws Throwable {
    mockTestReachability("Connection error");
    assertThrows(TimeoutException.class, () -> extension.beforeAll(null));
  }

  @Test
  public void testPrepareContainerFailsOnCopyToContainerException() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    doThrow(DockerException.class).when(dockerClient).copyToContainer(isA(Path.class), any(), any());

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
    assertTrue(ex.getMessage().contains("Could not copy files"));
  }

  @Test
  public void testPrepareContainerFailsOnExecCreateException() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), any()))
            .thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testPrepareContainerFailsOnExecStartException() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), any()))
            .thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testPrepareContainerFailsOnExecuteScript() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    mockExecuteScript("setup.sql", "Failure");

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> extension.beforeAll(null));
    assertTrue(ex.getMessage().contains("Evaluation of SQL script"));
  }

  @Test
  public void testInitializeSuccessful() throws Throwable {
    mockAndExecuteSuccessfulInitialization();

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysqladmin", "-uroot", "-proot", "ping"}), any(), any(), any());
    verify(dockerClient).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/setup.sql"}), any(), any(), any());
    verify(dockerClient, times(2)).execStart(any());
  }

  @Test
  public void testInitializeSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    extensionWithoutScripts.beforeAll(null);

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysqladmin", "-uroot", "-proot", "ping"}), any(), any(), any());
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

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> extension.afterEach(null));
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testTruncateFailsOnExecStartException() throws Throwable {
    when(dockerClient.execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), any()))
            .thenReturn(execCreation("executionID"));
    when(dockerClient.execStart("executionID")).thenThrow(DockerException.class);
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> extension.afterEach(null));
    assertTrue(ex.getMessage().contains(COULD_NOT_EXECUTE_SQL_SCRIPT));
  }

  @Test
  public void testTruncateFailsOnExecuteScript() throws Throwable {
    mockExecuteScript("truncate.sql", "Failure");
    mockAndExecuteSuccessfulInitialization();

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> extension.afterEach(null));
    assertTrue(ex.getMessage().contains("Evaluation of SQL script"));
  }

  @Test
  public void testTruncateSuccessful() throws Throwable {
    mockExecuteScript("truncate.sql", "");
    mockAndExecuteSuccessfulInitialization();
    extension.afterEach(null);

    verify(dockerClient).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), any(), any(), any());
  }

  @Test
  public void testTruncateSuccessfulWithoutScripts() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    extensionWithoutScripts.beforeAll(null);
    extensionWithoutScripts.afterEach(null);

    verify(dockerClient, never()).execCreate(any(), eq(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/truncate.sql"}), any());
  }

  private void mockAndExecuteSuccessfulInitialization() throws Throwable {
    mockTestReachability(MYSQLD_IS_ALIVE);
    mockExecuteScript("setup.sql", "");
    extension.beforeAll(null);
  }

  private void mockStartContainer() throws Exception {
    when(dockerClient.ping()).thenReturn("OK");
    when(dockerClient.createContainer(any())).thenReturn(ContainerCreation.builder().id("containerID").build());
    // dockerClient.startContainer() returns void, thus, no need to mock it.
  }

  private void mockTestReachability(String output) throws Exception {
    mockStartContainer();
    mockExecute(new String[]{"mysqladmin", "-uroot", "-proot", "ping"}, output);
  }

  private void mockExecuteScript(String name, String output) throws Exception {
    mockExecute(new String[]{"mysql", "-uroot", "-proot", "-e", "source /tmp/" + name}, output);
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
