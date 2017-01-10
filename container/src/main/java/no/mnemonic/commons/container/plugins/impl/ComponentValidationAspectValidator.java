package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.ValidationAspect;
import no.mnemonic.commons.component.ValidationContext;
import no.mnemonic.commons.container.plugins.ComponentValidator;

public class ComponentValidationAspectValidator implements ComponentValidator {

  @Override
  public boolean appliesTo(Object obj) {
    return obj != null && obj instanceof ValidationAspect;
  }

  @Override
  public void validate(ValidationContext ctx, Object obj) {
    if (obj == null) return;
    if (ctx == null) return;
    if (!(obj instanceof ValidationAspect)) throw new IllegalArgumentException("Invalid type: " + obj.getClass());
    ((ValidationAspect) obj).validate(ctx);
  }
}
