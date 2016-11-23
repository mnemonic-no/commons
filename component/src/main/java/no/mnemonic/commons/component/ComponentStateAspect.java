package no.mnemonic.commons.component;

public interface ComponentStateAspect {

  /**
   * This method may be called arbitrarily often, and should cause the called component
   * to generate an updated report on its current status.
   *
   * See {@link ComponentStatus} for details on what this report should contain.
   *
   * @return a component status message, providing the current state and health of the component
   */
  ComponentState getComponentState();

}
