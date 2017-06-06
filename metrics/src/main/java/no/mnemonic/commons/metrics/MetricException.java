package no.mnemonic.commons.metrics;

/**
 * Exceptions when writing metrics
 */
public class MetricException extends Exception {

  private static final long serialVersionUID = -3796128166023410532L;

  public MetricException() {
  }

  public MetricException(String s) {
    super(s);
  }

  public MetricException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public MetricException(Throwable throwable) {
    super(throwable);
  }
}
