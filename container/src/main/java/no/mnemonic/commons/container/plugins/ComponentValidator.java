package no.mnemonic.commons.container.plugins;

import no.mnemonic.commons.component.ValidationContext;

/**
 * Use this interface to register custom components which can validate the beans of the container
 * Validation is performed before starting any components.
 *
 * @see ComponentLifecycleHandler
 */
public interface ComponentValidator {

  boolean appliesTo(Object obj);

  void validate(ValidationContext ctx, Object obj);

}
