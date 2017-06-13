package no.mnemonic.commons.utilities.lambda;

/**
 * A callable task with a checked exception signature
 */
public interface ExceptionalTask <E extends Exception> {
  void call() throws E;
}
