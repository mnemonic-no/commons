package no.mnemonic.commons.container.plugins;

import java.util.Collection;

/**
 * Use this interface to extract dependencies from your container components.
 * During lifecycle management, any component C which is listed as dependency of parent component A, will
 * be started before A.
 *
 * Likewise, during shutdown, component A will be shut down before any of its dependencies (C).
 *
 * @see ComponentLifecycleHandler
 */
public interface ComponentDependencyResolver {

  /**
   * @param obj object to find dependencies for
   * @return the dependencies this resolver knows about for the given component, or an empty collection if no dependencies found/resolved.
   */
  Collection<?> resolveDependencies(Object obj);

}
