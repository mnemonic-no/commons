package no.mnemonic.commons.utilities;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * A chainable iterator that lazily executes deferred iterator-producing operations.
 * Each operation supplies an {@link Iterator} when needed, allowing delayed evaluation
 * and composition of multiple iterator sources. Can be used for chaining
 * fetch operations from Cassandra DB.
 *
 * <p>Use {@link Builder} to construct a chain by adding operations that each return
 * an {@code Iterator<T>}. The chain will iterate through all elements of the first
 * non-empty iterator before advancing to the next.</p>
 *
 * @param <T> the type of elements returned by this iterator
 */
public class DeferredIteratorChain<T> implements Iterator<T>, Iterable<T> {

  private final Queue<Supplier<Iterator<T>>> operations = new LinkedList<>();
  private Iterator<T> current = Collections.emptyIterator();

  private DeferredIteratorChain() {
    // private constructor, use Builder instead
  }

  /**
   * Creates a new builder for constructing a {@code DeferredIteratorChain}.
   *
   * @param <T> the type of elements in the iterator
   * @return a new {@link Builder} instance
   */
  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Builder for constructing {@link DeferredIteratorChain} instances.
   *
   * @param <T> the element type
   */
  public static class Builder<T> {
    private final DeferredIteratorChain<T> chain = new DeferredIteratorChain<>();

    /**
     * Adds a deferred operation that supplies an {@link Iterator}.
     *
     * @param operation supplier that provides an iterator when invoked
     * @return this builder for method chaining
     * @throws NullPointerException if {@code operation} is null
     */
    public Builder<T> addOperation(Supplier<Iterator<T>> operation) {
      if (operation == null) {
        throw new NullPointerException("Operation must not be null");
      }
      chain.operations.add(operation);
      return this;
    }

    /**
     * Builds and returns the {@link DeferredIteratorChain}. Reusing build on
     * the same object may cause unpredictable behavior.
     *
     * @return a new chain ready for iteration
     */
    public DeferredIteratorChain<T> build() {
      return chain;
    }
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }

  @Override
  public boolean hasNext() {
    // Advance until a non-empty iterator is found
    while (!current.hasNext() && !operations.isEmpty()) {
      Supplier<Iterator<T>> supplier = operations.poll();
      Iterator<T> nextIt = (supplier != null ? supplier.get() : null);
      current = (nextIt != null ? nextIt : Collections.emptyIterator());
    }
    return current.hasNext();
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more elements in the DeferredIteratorChain");
    }
    return current.next();
  }
}


