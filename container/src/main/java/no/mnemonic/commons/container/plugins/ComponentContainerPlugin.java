package no.mnemonic.commons.container.plugins;

import java.util.Map;

/**
 * Use this interface to register container plugins.
 * Plugins will be invoked before the container is validated or started.
 */
public interface ComponentContainerPlugin {

  boolean appliesTo(Object obj);

  void registerBeans(Map<String, Object> matchingBeans);

}
