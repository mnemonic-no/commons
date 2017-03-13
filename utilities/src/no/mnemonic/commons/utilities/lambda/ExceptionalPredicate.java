package no.mnemonic.commons.utilities.lambda;

public interface ExceptionalPredicate<A, E extends Exception> {
  boolean test(A a) throws E;
}
