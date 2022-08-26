package no.mnemonic.commons.logging;

/**
 * Small context which can be wrapped around a piece of deprecated code. Every log statement inside the scope
 * of the context will be annotated with the 'deprecated' context variable.
 * <p>
 * Usage:
 * <pre>
 * try (DeprecatedLoggingContext ignored = DeprecatedLoggingContext.create()) {
 *   // Every log statement inside this block will be annotated
 *   // with the 'deprecated' context variable.
 *   LOGGER.warning("This code path is deprecated!");
 * }
 * </pre>
 */
public class DeprecatedLoggingContext implements AutoCloseable {

  public static final String DEPRECATED = "deprecated";

  private final boolean contextAlreadySet;

  private DeprecatedLoggingContext() {
    contextAlreadySet = Logging.getLoggingContext().containsKey(DEPRECATED);
    if (!contextAlreadySet) {
      Logging.getLoggingContext().put(DEPRECATED, "true");
    }
  }

  public static DeprecatedLoggingContext create() {
    return new DeprecatedLoggingContext();
  }

  @Override
  public void close() {
    // Don't remove the variable from the LoggingContext if another component
    // has already set it outside the scope of the current DeprecationContext.
    if (!contextAlreadySet) {
      Logging.getLoggingContext().remove(DEPRECATED);
    }
  }
}