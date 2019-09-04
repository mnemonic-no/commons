package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.Dependency;
import no.mnemonic.commons.component.DependencyProvider;
import no.mnemonic.commons.container.plugins.ComponentDependencyResolver;
import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.ListUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static no.mnemonic.commons.utilities.ObjectUtils.ifNotNullDo;
import static no.mnemonic.commons.utilities.collections.SetUtils.set;

public class MethodAnnotationDependencyResolver implements ComponentDependencyResolver {

  private static final Logger LOGGER = Logging.getLogger(MethodAnnotationDependencyResolver.class);

  private final Map<Object, Object> providerMap = new HashMap<>();

  @Override
  public void scan(Collection<Object> objects) {
    ListUtils.list(objects).stream()
        .filter(o -> o instanceof DependencyProvider)
        .map(o -> (DependencyProvider) o)
        .forEach(p -> ifNotNullDo(p.getProvidedDependency(), d -> providerMap.put(d, p)));
  }

  @Override
  public Collection<Object> resolveDependencies(Object object) {
    Collection<Object> dependencies = new HashSet<>();
    if (object == null) return dependencies;
    for (Method m : findDependencyGetters(object.getClass())) {
      try {
        m.setAccessible(true);
        Object dep = m.invoke(object);
        if (dep == null) continue;
        dependencies.add(dep);
        if (providerMap.containsKey(dep)) {
          dependencies.add(providerMap.get(dep));
        }
      } catch (IllegalAccessException | InvocationTargetException e) {
        LOGGER.warning(String.format("Error checking for dependency: %s", object));
      }
    }
    return dependencies;
  }

  private Set<Method> findDependencyGetters(Class objectClass) {
    return set(objectClass.getMethods()).stream()
        .filter(m -> m.isAnnotationPresent(Dependency.class))
        .filter(m -> m.getParameterTypes().length == 0)
        .collect(Collectors.toSet());
  }

}
