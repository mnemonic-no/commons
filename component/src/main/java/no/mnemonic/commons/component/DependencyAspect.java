package no.mnemonic.commons.component;

import java.util.Collection;

public interface DependencyAspect extends LifecycleAspect {

  /**
   * Method must be called AFTER constructing the beans, before lifecycle methods are performed
   *
   * @return a list of all lifecycle dependencies this object has
   */
  Collection<DependencyAspect> getDependencies();

}