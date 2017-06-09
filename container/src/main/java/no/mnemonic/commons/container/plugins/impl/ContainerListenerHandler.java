package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.container.ComponentContainer;
import no.mnemonic.commons.container.ContainerListener;
import no.mnemonic.commons.container.plugins.ComponentContainerPlugin;

import java.util.Map;

import static no.mnemonic.commons.utilities.collections.MapUtils.map;

public class ContainerListenerHandler implements ComponentContainerPlugin {

  private final ComponentContainer parentContainer;

  public ContainerListenerHandler(ComponentContainer parentContainer) {
    if (parentContainer == null) throw new IllegalStateException("Parent container not set");
    this.parentContainer = parentContainer;
  }

  @Override
  public boolean appliesTo(Object obj) {
    return ContainerListener.class.isInstance(obj);
  }

  @Override
  public void registerBeans(Map<String, Object> matchingBeans) {
    map(matchingBeans).forEach((k, v) -> parentContainer.addContainerListener((ContainerListener) v));
  }


}
