package no.mnemonic.commons.component;

import org.junit.Test;

import static no.mnemonic.commons.utilities.collections.ListUtils.list;
import static org.junit.Assert.*;

public class ValidationContextTest implements ValidationAspect {

  @Override
  public void validate(ValidationContext ctx) {
  }

  @Test
  public void testValidationContextIsValid() {
    assertTrue(new ValidationContext().isValid());
    assertTrue(new ValidationContext().addWarning(this, "warning").isValid());
    assertFalse(new ValidationContext().addError(this, "error").isValid());
  }

  @Test
  public void testValidationContextListErrors() {
    assertEquals(0, new ValidationContext().getErrors().size());
    assertEquals(0, new ValidationContext().getWarnings().size());
    assertEquals(list(this + ": warning"), new ValidationContext().addWarning(this, "warning").getWarnings());
    assertEquals(list(this + ": error"), new ValidationContext().addError(this, "error").getErrors());
  }
  @Test
  public void testValidationContextReset() {
    assertEquals(list(), new ValidationContext().addWarning(this, "warning").reset().getWarnings());
    assertEquals(list(), new ValidationContext().addError(this, "error").reset().getErrors());
  }
}
