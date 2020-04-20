package no.mnemonic.commons.junit.docker;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

public class SingletonDockerResourceWrapperTest {

  private static DockerResource resource = mock(DockerResource.class);

  @Rule
  public SingletonDockerResourceWrapper<DockerResource> wrapper = SingletonDockerResourceWrapper.builder()
          .setDockerResource(resource)
          .build();

  @AfterClass
  public static void verifyAfterWasNotCalled() throws Throwable {
    //before() was called once per test
    verify(resource, times(3)).before();
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

  @Test
  public void testReturnWrappedResource() {
    assertSame(resource, wrapper.getDockerResource());
  }
}
