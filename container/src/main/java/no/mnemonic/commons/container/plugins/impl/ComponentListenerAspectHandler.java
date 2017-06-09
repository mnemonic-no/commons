package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.ComponentListenerAspect;
import no.mnemonic.commons.container.ComponentContainer;
import no.mnemonic.commons.container.plugins.ComponentContainerPlugin;

import java.util.Map;

import static no.mnemonic.commons.utilities.collections.MapUtils.map;

/**
 * This handler will register any components implementing ComponentListenerAspect, and add the parent container
 * as a component listener to those components. This way, any ComponentListenerAspect bean (which is behaving correctly)
 * will notify its parent container when shutting down.
 */
public class ComponentListenerAspectHandler implements ComponentContainerPlugin {

  private final ComponentContainer parentContainer;

  public ComponentListenerAspectHandler(ComponentContainer parentContainer) {
    if (parentContainer == null) throw new IllegalStateException("Parent container not set");
    this.parentContainer = parentContainer;
  }

  @Override
  public boolean appliesTo(Object obj) {
    return ComponentListenerAspect.class.isInstance(obj);
  }

  @Override
  public void registerBeans(Map<String, Object> matchingBeans) {
    map(matchingBeans).forEach((k, v) -> ((ComponentListenerAspect)v).addComponentListener(parentContainer));
  }


}
