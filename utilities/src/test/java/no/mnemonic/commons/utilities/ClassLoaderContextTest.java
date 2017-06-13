package no.mnemonic.commons.utilities;

import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.assertSame;

public class ClassLoaderContextTest {

  @Test(expected = IllegalArgumentException.class)
  public void testClassLoaderContextOfNullClassLoader() {
    //noinspection RedundantCast,EmptyTryBlock
    try (ClassLoaderContext ignored = ClassLoaderContext.of((ClassLoader) null)) {
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testClassLoaderContextOfNullObject() {
    //noinspection RedundantCast,EmptyTryBlock
    try (ClassLoaderContext ignored = ClassLoaderContext.of((Object) null)) {
    }
  }

  @Test
  public void testClassLoaderContextResetsAfterFinish() {
    ClassLoader threadClassLoader =  Thread.currentThread().getContextClassLoader();
    ClassLoader newClassLoader = new URLClassLoader(new URL[]{});
    assertSame(threadClassLoader, Thread.currentThread().getContextClassLoader());
    try (ClassLoaderContext ignored = ClassLoaderContext.of(newClassLoader)) {
      assertSame(newClassLoader, Thread.currentThread().getContextClassLoader());
    }
    assertSame(threadClassLoader, Thread.currentThread().getContextClassLoader());
  }
  @Test
  public void testClassLoaderContextResetsAfterException() {
    ClassLoader threadClassLoader =  Thread.currentThread().getContextClassLoader();
    ClassLoader newClassLoader = new URLClassLoader(new URL[]{});
    assertSame(threadClassLoader, Thread.currentThread().getContextClassLoader());
    try {
      try (ClassLoaderContext ignored = ClassLoaderContext.of(newClassLoader)) {
        assertSame(newClassLoader, Thread.currentThread().getContextClassLoader());
        throw new IllegalStateException();
      }
    } catch (IllegalStateException ignored) {}
    assertSame(threadClassLoader, Thread.currentThread().getContextClassLoader());
  }
}
