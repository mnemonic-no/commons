package no.mnemonic.commons.component;

public class ComponentException extends RuntimeException {

  static final long serialVersionUID = 4562539593025549508L;

  public ComponentException(String msg) {
    super(msg);
  }

  public ComponentException(Throwable ex) {
    super(ex);
  }

  public ComponentException(String msg, Throwable ex) {
    super(msg, ex);
  }
}
