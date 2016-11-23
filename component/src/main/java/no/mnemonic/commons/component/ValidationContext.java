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

  public ValidationContext addWarning(ValidationAspect component, Object msg) {
    warnings.add(component + ": " + msg);
    return this;
  }

  public ValidationContext addError(ValidationAspect component, Object msg) {
    errors.add(component + ": " + msg);
    return this;
  }

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