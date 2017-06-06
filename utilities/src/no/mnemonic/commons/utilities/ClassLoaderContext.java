package no.mnemonic.commons.utilities;

/**
 * A classloader context simplifies switching the current thread classloader for the
 * duration of a code block, and ensures that the current thread classloader is reset
 * to original value at the end of the code block.
 *
 * <code>
 *   Object obj = loadObjectFromNonSystemClassLoader(...);
 *   try (ClassLoaderContext ctx = ClassLoaderContext.of(obj)) {
 *     //invoke code which needs to use the classloader of "obj"
 *   }
 * </code>
 */
public class ClassLoaderContext implements AutoCloseable {

  private final ClassLoader contextClassLoader;
  private final ClassLoader originalClassloader;

  private ClassLoaderContext(ClassLoader requestedClassloader) {
    if (requestedClassloader == null) throw new IllegalArgumentException("No classloader");
    this.contextClassLoader = requestedClassloader;
    this.originalClassloader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(requestedClassloader);
  }

  @Override
  public void close() {
    Thread.currentThread().setContextClassLoader(originalClassloader);
  }

  public static ClassLoaderContext of(ClassLoader cl) {
    return new ClassLoaderContext(cl);
  }

  public static ClassLoaderContext of(Object obj) {
    if (obj == null) throw new IllegalArgumentException("No object");
    return new ClassLoaderContext(obj.getClass().getClassLoader());
  }

  public ClassLoader getContextClassLoader() {
    return contextClassLoader;
  }
}