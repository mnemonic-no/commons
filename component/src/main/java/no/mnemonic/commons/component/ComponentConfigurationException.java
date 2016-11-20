package no.mnemonic.commons.component;

public class ComponentConfigurationException extends ComponentException {

  static final long serialVersionUID = 4193531929479631570L;

  public ComponentConfigurationException(ValidationContext ctx) {
    super("Configuration validation failed: " + ctx.getErrors());
  }

  public ComponentConfigurationException(String msg) {
    super(msg);
  }

  public ComponentConfigurationException(Throwable ex) {
    super(ex);
  }

  public ComponentConfigurationException(String msg, Throwable ex) {
    super(msg, ex);
  }
}
