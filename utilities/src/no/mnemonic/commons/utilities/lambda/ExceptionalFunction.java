package no.mnemonic.commons.utilities.lambda;

public interface ExceptionalFunction<A, B, E extends Exception> {
  B apply(A a) throws E;
}
