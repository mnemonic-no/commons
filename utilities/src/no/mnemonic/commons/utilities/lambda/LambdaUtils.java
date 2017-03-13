package no.mnemonic.commons.utilities.lambda;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class LambdaUtils {
  private LambdaUtils() {
  }

  /**
   * Call provided task, ignore any exception thrown.
   * Convenience method to call a method/lambda without having to wrap with try/catch
   *
   * @param callable task to call
   * @return true if task was successfull, false if exception was caught
   */
  public static boolean tryTo(ExceptionalTask callable) {
    return tryTo(callable, e->{});
  }

  /**
   * Call provided task, ignore any exception thrown, instead passing it to a provided exception handler.
   * Convenience method to call a method/lambda without having to wrap with try/catch
   *
   * <code>
   *   LambdaUtils.tryTo(()->doSomethingThatMightThrowAnException(), error->LOGGER.warn("Error doing something", error));
   * </code>
   *
   * @param callable task to call
   * @param onException consumer to provide any exception caught
   * @return true if task was successfull, false if exception was caught
   */
  public static boolean tryTo(ExceptionalTask callable, Consumer<Throwable> onException) {
    if (callable == null) return false;
    try {
      callable.call();
      return true;
    } catch (Exception e) {
      notifyException(onException, e);
      return false;
    }
  }

  /**
   * Try to perform operation on multiple values, ignoring any exception thrown.
   * Convenience method to call forEach on a collection of values without having to use try/catch in lambda
   *
   * @param values values
   * @param consumer consumer to handle value, which may throw exception
   * @param <T> value type
   */
  public static <T> void forEachTry(Collection<T> values, ExceptionalConsumer<T, ? extends Exception> consumer) {
    forEachTry(values, consumer, e->{});
  }

  /**
   * Try to perform operation on multiple values, ignoring any exception thrown, instead pass any error to exception consumer.
   * Convenience method to call forEach on a collection of values without having to use try/catch in lambda
   *
   * @param values values
   * @param consumer consumer to handle value, which may throw exception
   * @param onException exception handler to pass any exception to. Might be called once for every invocation of consumer.
   * @param <T> value type
   */
  public static <T, E extends Exception> void forEachTry(Collection<T> values, ExceptionalConsumer<T, E> consumer, Consumer<Throwable> onException) {
    if (values == null) return;
    if (consumer == null) return;
    values.forEach(v->{
      try {
        consumer.accept(v);
      } catch (Throwable t) {
        notifyException(onException, t);
      }
    });
  }

  /**
   * Wrap stream into a TryStream.
   * A TryStream allows using map/filter lambdas which throws checked exceptions.
   * Any exception thrown will be caught and rethrown by this method, this is just a convenience method
   * to avoid having to use try/catch inside your lambdas.
   *
   * @param stream any stream
   * @param <T> stream type
   * @param <E> checked exception
   * @return a TryStream wrapping the stream
   * @see TryStream
   */
  public static <T, E extends Exception> TryStream<T, E> tryStream(Stream<T> stream) {
    if (stream == null) return null;
    return new TryStreamImpl<>(stream);
  }

  //helpers

  private static void notifyException(Consumer<Throwable> onException, Throwable t) {
    if (onException == null) return;
    try {
      onException.accept(t);
    } catch (Throwable ignored) {
    }
  }

}
