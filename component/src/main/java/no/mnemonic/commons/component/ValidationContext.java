package no.mnemonic.commons.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationContext {

  private List<String> warnings = new ArrayList<>();
  private List<String> errors = new ArrayList<>();

  public boolean isValid() {
    return errors.size() == 0;
  }

  /**
   * @param component The component reporting the warning
   * @param msg the warning message
   * @return the context
   */
  public ValidationContext addWarning(Object component, Object msg) {
    warnings.add(component + ": " + msg);
    return this;
  }

  /**
   * @param component The component reporting the error
   * @param msg the error message
   * @return the context
   */
  public ValidationContext addError(Object component, Object msg) {
    errors.add(component + ": " + msg);
    return this;
  }

  /**
   * Clear all errors and warnings
   * @return the context
   */
  public ValidationContext reset() {
    warnings.clear();
    errors.clear();
    return this;
  }

  public List<String> getWarnings() {
    return Collections.unmodifiableList(warnings);
  }

  public List<String> getErrors() {
    return Collections.unmodifiableList(errors);
  }
}