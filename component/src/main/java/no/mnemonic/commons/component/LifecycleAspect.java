package no.mnemonic.commons.component;

public interface LifecycleAspect {
  /**
   * A component should initialize any resources it needs at this time, and prepare for operation. Active components
   * should start operation at this time.
   *
   * Note that other components may start before this component, and it should make sure that it properly handles
   * requests/calls from other components even if it is not in it's started state.
   */
  void startComponent();

  /**
   * A component should close and release any resources it holds at this time, and prepare for destruction. Active
   * components should stop working and release/end threads.
   *
   * Note that other components may stop after this component, and it should make sure that it properly handles
   * requests/calls from other components, even if it has already stopped.
   *
   */
  void stopComponent();


}