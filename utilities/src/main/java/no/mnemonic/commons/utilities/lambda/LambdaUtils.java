package no.mnemonic.commons.utilities.lambda;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class LambdaUtils {
  private LambdaUtils() {
  }


  /**
   * Simple helper to wait for a predicate to return true. Will test predicate every 100ms until true or timeout.
   *
   * @param booleanSupplier the predicate
   * @param waitTime how long to wait before giving up
   * @param timeUnit time unit of waitTime
   * @return true if predicate returns true within timeout, false if timed out without getting true value
   * @throws InterruptedException if interrupted during sleep.
   */
  public static boolean waitFor(BooleanSupplier booleanSupplier, long waitTime, TimeUnit timeUnit)  throws InterruptedException {
    long timeout = System.currentTimeMillis() + timeUnit.toMillis(waitTime);
    while (System.currentTimeMillis() < timeout) {
      if (booleanSupplier.getAsBoolean()) return true;
      Thread.sleep(100);
    }
    return false;
  }

  /**
   * Call provided task, ignore any exception thrown.
   * Convenience method to call a method/lambda without having to wrap with try/catch
   *
   * @param callable task to call
   * @return true if task was successful, false if exception was caught
   */
  public static boolean tryTo(ExceptionalTask callable) {
    return tryTo(callable, e->{});
  }

  /**
   * Call provided task, ignore any exception thrown, instead passing it to a provided exception handler.
   * Convenience method to call a method/lambda without having to wrap with try/catch
   *
   * <code>
   *   LambdaUtils.tryTo(()-&gt;doSomethingThatMightThrowAnException(), error-&gt;LOGGER.warn("Error doing something", error));
   * </code>
   *
   * @param callable task to call
   * @param onException consumer to provide any exception caught
   * @return true if task was successful, false if exception was caught
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
   * Invoke callable to fetch result and return it. If exception is thrown, return default value instead.
   * Use this method to avoid try/catch blocks where the expected exception should result in a default value.
   *
   * @param supplier callable to fetch result from
   * @param defaultValue value to return if callable fails
   * @param <T> value type
   * @return the value from the callable, or defaultValue on exception
   */
  public static <T> T tryResult(Callable<T> supplier, T defaultValue)  {
    return tryResult(supplier, ()->defaultValue, e->{});
  }

  /**
   * Invoke callable to fetch result and return it. If exception is thrown, return value from defaultValueSupplier instead.
   * Notify onException of any exception caught.
   * Use this method to avoid try/catch blocks where the expected exception should result in a default value.
   *
   * @param supplier callable to fetch result from
   * @param defaultValueSupplier supplier to fetch default value from
   * @param onException exception consumer to notify on exception
   * @param <T> value type
   * @return the value from the callable, or defaultValue on exception
   *
   * @see #tryResult(Callable, Object)
   */
  public static <T> T tryResult(Callable<T> supplier, Supplier<T> defaultValueSupplier, Consumer<Throwable> onException)  {
    if (supplier == null) throw new IllegalArgumentException("supplier not set");
    if (defaultValueSupplier == null) throw new IllegalArgumentException("defaultValueSupplier not set");
    if (onException == null) throw new IllegalArgumentException("onException not set");
    try {
      return supplier.call();
    } catch (Exception e) {
      notifyException(onException, e);
      return defaultValueSupplier.get();
    }
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

  public static <T, E extends Exception> TryStream<T, E> tryStream(Collection<T> collection) {
    if (collection == null) return null;
    return tryStream(collection.stream());
  }

    //helpers

  private static void notifyException(Consumer<Throwable> onException, Throwable t) {
    if (onException == null) return;
    try {
      onException.accept(t);
    } catch (Throwable ignored) {
      //ignore
    }
  }

}
