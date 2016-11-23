package no.mnemonic.commons.component;

/**
 * This aspect of an object reflects that the object is able to perform self-validation.
 * Validation is typically called after initialization of an object collection, before
 * actually starting a set of services. The validation may verify that the components/objects
 * themselves feel fit for action before starting the components.
 *
 * @author jar
 * @version $Id$
 * @since 03.aug.2004 09:12:34
 */
public interface ValidationAspect {

  /**
   * This should trigger a self-validation of the object.
   * Errors and warnings should be logged to the ValidationContext.
   * Errors will cause the system to abandon the initialization attempt.
   * Warnings will only be printed.
   *
   * @param ctx the validation context to register errors and warnings in.
   */
  void validate(ValidationContext ctx);
}