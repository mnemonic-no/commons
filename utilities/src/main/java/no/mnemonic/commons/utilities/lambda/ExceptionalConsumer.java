package no.mnemonic.commons.utilities.lambda;

/**
 * Consumer interface with checked exception
 * @param <T>
 */
public interface ExceptionalConsumer<T, E extends Exception> {
  void accept(T input) throws E;
}
