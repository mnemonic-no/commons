package no.mnemonic.commons.utilities.lambda;

import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

class TryStreamImpl<T, E extends Exception> implements TryStream<T, E> {
  private final Stream<T> stream;

  TryStreamImpl(Stream<T> stream) {
    this.stream = stream;
  }

  @Override
  public <R> TryStream<R, E> map(ExceptionalFunction<? super T, ? extends R, E> mapper) {
    return new TryStreamImpl<>(stream.map(v -> applyExceptionalFunction(mapper, v)));
  }

  @Override
  public Stream<T> stream() {
    return stream;
  }

  @Override
  public LongStream mapToLong(ExceptionalFunction<? super T, Long, E> function) throws E {
    return stream.mapToLong(v -> applyExceptionalFunction(function, v));
  }

  @Override
  public IntStream mapToInt(ExceptionalFunction<? super T, Integer, E> function) throws E {
    return stream.mapToInt(v -> applyExceptionalFunction(function, v));
  }

  @Override
  public TryStream<T, E> filter(ExceptionalPredicate<? super T, E> predicate) {
    return new TryStreamImpl<>(stream.filter(o -> {
      try {
        return predicate.test(o);
      } catch (Exception e) {
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        throw new StreamException(e);
      }
    }));
  }

  @Override
  public <R, A> R collect(Collector<? super T, A, R> collector) throws E {
    try {
      return stream.collect(collector);
    } catch (StreamException e) {
      //noinspection unchecked
      throw (E) e.getCause();
    }
  }

  @Override
  public void forEach(ExceptionalConsumer<? super T, E> consumer) throws E {
    try {
      stream.forEach(el -> {
        try {
          consumer.accept(el);
        } catch (Exception e) {
          if (e instanceof RuntimeException) throw (RuntimeException) e;
          throw new StreamException(e);
        }
      });
    } catch (StreamException e) {
      //noinspection unchecked
      throw (E) e.getCause();
    }
  }


  private <R> R applyExceptionalFunction(ExceptionalFunction<? super T, ? extends R, E> mapper, T v) {
    try {
      return mapper.apply(v);
    } catch (Exception e) {
      if (e instanceof RuntimeException) throw (RuntimeException) e;
      throw new StreamException(e);
    }
  }

  private static class StreamException extends RuntimeException {
    StreamException(Throwable cause) {
      super(cause);
    }
  }
}
