package no.mnemonic.commons.container.plugins;

import no.mnemonic.commons.component.ComponentNamingAspect;

import java.util.Map;

public class ComponentNamingPlugin implements ComponentContainerPlugin<ComponentNamingAspect> {

  @Override
  public Class<ComponentNamingAspect> getBeanInterface() {
    return ComponentNamingAspect.class;
  }

  @Override
  public void handleBeans(Map<String, ComponentNamingAspect> matchingBeans) {
    if (matchingBeans == null) return;
    matchingBeans.entrySet().forEach(e->e.getValue().setComponentName(e.getKey()));
  }
}
