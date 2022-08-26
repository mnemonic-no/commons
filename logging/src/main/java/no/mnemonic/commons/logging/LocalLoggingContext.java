package no.mnemonic.commons.logging;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Use this context to easily annotate all logs from a limited code block with certain context variables.
 * <p>
 * Example:
 * <code>
 * try (LocalLoggingContext llc = LocalLoggingContext.using("correlationID", myCorrelationID)) {
 * //  this will be logged with correlationID SET
 * }
 * //here the correlationID should be removed again....
 * </code>
 * If setting a context variable overwrites an existing variable, this will be reset to the original value when closing the context.
 * </p>
 *
 * <p>
 *   When working with multiple threads, use {@link #duplicate()} to duplicate an existing context, or
 *   the {@link #running(Runnable)} to copy the logging context to the context of the running task:
 *   <code>
 *   try (LocalLoggingContext llc = LocalLoggingContext.using("correlationID", myCorrelationID)) {
 *      executor.submit(llc.running(()-&gt;{
 *        //  this will be logged with correlationID SET
 *      }));
 *   }
 *   </code>
 * </p>
 *
 */
public class LocalLoggingContext implements AutoCloseable {

  //map of changed keys, with the original value
  private final Map<String, String> oldContext = new HashMap<>();
  //map of changed keys, with the new value
  private final Map<String, String> newContext = new HashMap<>();
  //the original (untouched) context at the time when the context was created
  private final Map<String, String> originalContext = new HashMap<>();
  private final LoggingContext ctx;

  private LocalLoggingContext() {
    ctx = Logging.getLoggingContext();
    originalContext.putAll(ctx.getAll());
  }

  /**
   * Add variable to context. Any variable added here will be cleared when the context is closed.
   *
   * @param key      variable key
   * @param newValue variable value
   * @return this context, to allow method chaining.
   */
  public LocalLoggingContext using(String key, String newValue) {
    if (key == null) return this;
    this.oldContext.put(key, originalContext.get(key));
    this.newContext.put(key, newValue);
    ctx.put(key, newValue);
    return this;
  }

  /**
   * Create a new LocalLoggingContext in the current thread duplicating this context
   * Example usage:
   * <code>
   * try (LocalLoggingContext ctx = LocalLoggingContext.create().using("correlationID", UUID.randomUUID().toString)) {
   * executor.submit(()-&gt;{
   * try (LocalLoggingContext tctx = ctx.duplicate()) {
   * //here, the correlationID is set in the LoggingContext of this thread too!
   * }
   * });
   * }
   * </code>
   *
   * @return the new LocalLoggingContext, setting all parameters from this context into the current thread.
   */
  public LocalLoggingContext duplicate() {
    LocalLoggingContext duplicateContext = new LocalLoggingContext();
    this.originalContext.forEach(duplicateContext::using);
    this.newContext.forEach(duplicateContext::using);
    return duplicateContext;
  }

  /**
   * Run a callable with the logging context set by this LocalLoggingContext
   * Example usage:
   * <code>
   * try (LocalLoggingContext ctx = LocalLoggingContext.create().using("correlationID", UUID.randomUUID().toString)) {
   * executor.submit(ctx.running(()-&gt;{
   * //here, the correlationID is set in the LoggingContext of this thread too!
   * }));
   * }
   * </code>
   */
  public <T> Callable<T> running(Callable<T> task) {
    return () -> {
      try (LocalLoggingContext ignored = LocalLoggingContext.this.duplicate()) {
        return task.call();
      }
    };
  }

  /**
   * Run a callable with the logging context set by this LocalLoggingContext
   * Example usage:
   * <code>
   * try (LocalLoggingContext ctx = LocalLoggingContext.create().using("correlationID", UUID.randomUUID().toString)) {
   * executor.submit(ctx.running(()-&gt;{
   * //here, the correlationID is set in the LoggingContext of this thread too!
   * }));
   * }
   * </code>
   */
  public Runnable running(Runnable task) {
    return () -> {
      try (LocalLoggingContext ignored = this.duplicate()) {
        task.run();
      }
    };
  }

  @Override
  public void close() {
    oldContext.forEach((k, v) -> {
      if (v == null) ctx.remove(k);
      else ctx.put(k, v);
    });
  }

  /**
   * Create a new LocalLoggingContext.
   * Recommended usage is using try-with-resources to ensure the context is closed when done
   */
  public static LocalLoggingContext create() {
    return new LocalLoggingContext();
  }
}
