package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.ValidationAspect;
import no.mnemonic.commons.component.ValidationContext;
import no.mnemonic.commons.container.plugins.impl.ComponentValidationAspectValidator;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ComponentValidationAspectValidatorTest {

  @Test
  public void testAppliesTo() {
    ComponentValidationAspectValidator handler = new ComponentValidationAspectValidator();
    assertFalse(handler.appliesTo(null));
    assertFalse(handler.appliesTo("string"));
    assertFalse(handler.appliesTo(1));
    assertTrue(handler.appliesTo(new ValidationAspect() {
      @Override
      public void validate(ValidationContext ctx) {
      }
    }));
  }

  @Test
  public void testValidateNullContext() {
    ValidationAspect obj = mock(ValidationAspect.class);
    new ComponentValidationAspectValidator().validate(null, obj);
    verify(obj, never()).validate(any());
  }

  @Test
  public void testValidate() {
    ValidationAspect obj = mock(ValidationAspect.class);
    ValidationContext ctx = new ValidationContext();
    new ComponentValidationAspectValidator().validate(ctx, obj);
    verify(obj).validate(ctx);
  }

  @Test
  public void testValidateWithError() {
    ValidationAspect obj = mock(ValidationAspect.class);
    doAnswer(i->{
      ValidationContext ctx = i.getArgument(0);
      ctx.addError(obj, "error");
      return null;
    }).when(obj).validate(any());
    ValidationContext ctx = new ValidationContext();
    new ComponentValidationAspectValidator().validate(ctx, obj);
    verify(obj).validate(ctx);
    assertFalse(ctx.isValid());
    assertEquals(1, ctx.getErrors().size());
  }

  @Test
  public void testValidateWithWarning() {
    ValidationAspect obj = mock(ValidationAspect.class);
    doAnswer(i->{
      ValidationContext ctx = i.getArgument(0);
      ctx.addWarning(obj, "warning");
      return null;
    }).when(obj).validate(any());
    ValidationContext ctx = new ValidationContext();
    new ComponentValidationAspectValidator().validate(ctx, obj);
    verify(obj).validate(ctx);
    assertTrue(ctx.isValid());
    assertEquals(1, ctx.getWarnings().size());
  }

  @Test
  public void testValidateNullObject() {
    new ComponentValidationAspectValidator().validate(new ValidationContext(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateInvalidType() {
    new ComponentValidationAspectValidator().validate(new ValidationContext(), "string");
  }
}
