package no.mnemonic.commons.jupiter.docker;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

public class SingletonDockerExtensionWrapperTest {

  private static final DockerExtension wrappedExtension = mock(DockerExtension.class);

  @RegisterExtension
  public static SingletonDockerExtensionWrapper<DockerExtension> wrapper = SingletonDockerExtensionWrapper.builder()
          .setDockerExtension(wrappedExtension)
          .build();

  @AfterAll
  public static void verifyMockInteraction() throws Throwable {
    // beforeAll() was called once.
    verify(wrappedExtension, atMostOnce()).beforeAll(any());
    // beforeEach() was called once per test.
    verify(wrappedExtension, times(2)).beforeEach(any());
    // afterEach() was called once per test.
    verify(wrappedExtension, times(2)).afterEach(any());
    // afterAll() was never called.
    verify(wrappedExtension, never()).afterAll(any());
  }

  @Test
  public void testBeforeIsCalledBeforeEnteringTest() throws Throwable {
    verify(wrappedExtension, atMostOnce()).beforeAll(any());
    verify(wrappedExtension, atLeastOnce()).beforeEach(any());
  }

  @Test
  public void testReturnWrappedResource() {
    assertSame(wrappedExtension, wrapper.getDockerExtension());
  }
}
