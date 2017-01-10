package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.component.ValidationAspect;
import no.mnemonic.commons.container.plugins.ComponentLifecycleHandler;

public class ComponentLifecycleAspectHandler implements ComponentLifecycleHandler {

  @Override
  public boolean appliesTo(Object obj) {
    return obj != null && obj instanceof LifecycleAspect;
  }

  @Override
  public void startComponent(Object obj) {
    if (obj == null) return;
    if (!(obj instanceof LifecycleAspect)) throw new IllegalArgumentException("Invalid type: " + obj.getClass());
    ((LifecycleAspect)obj).startComponent();
  }

  @Override
  public void stopComponent(Object obj) {
    if (obj == null) return;
    if (!(obj instanceof LifecycleAspect)) throw new IllegalArgumentException("Invalid type: " + obj.getClass());
    ((LifecycleAspect)obj).stopComponent();
  }
}
