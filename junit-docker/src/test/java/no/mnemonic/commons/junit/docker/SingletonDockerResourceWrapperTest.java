package no.mnemonic.commons.junit.docker;

import org.junit.*;

import static org.mockito.Mockito.*;

public class SingletonDockerResourceWrapperTest {

  private static DockerResource resource = mock(DockerResource.class);

  @Rule
  public SingletonDockerResourceWrapper wrapper = SingletonDockerResourceWrapper.builder()
          .setDockerResource(resource)
          .build();

  @AfterClass
  public static void verifyAfterWasNotCalled() throws Throwable {
    //before() was called once per test
    verify(resource, times(2)).before();
    //after() was never called
    verify(resource, never()).after();
  }

  @Test
  public void testBeforeIsCalledBeforeEnteringTest() throws Throwable {
    verify(resource, atLeastOnce()).before();
  }

  @Test
  public void testAgain() throws Throwable {
    verify(resource, atLeastOnce()).before();
  }

}
