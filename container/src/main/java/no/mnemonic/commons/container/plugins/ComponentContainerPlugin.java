package no.mnemonic.commons.container.plugins;

import java.util.Map;

public interface ComponentContainerPlugin<T> {

  Class<T> getBeanInterface();

  void handleBeans(Map<String, T> matchingBeans);

}
