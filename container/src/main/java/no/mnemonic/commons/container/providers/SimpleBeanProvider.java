package no.mnemonic.commons.container.providers;

import no.mnemonic.commons.utilities.collections.MapUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleBeanProvider implements BeanProvider {

  private final Map<String, Object> components = new HashMap<>();

  public SimpleBeanProvider(Iterable<?> components) {
    if (components == null) throw new IllegalArgumentException("No components provided!");
    components.forEach(c->this.components.put(UUID.randomUUID().toString(), c));
  }

  @Override
  public <T> Optional<T> getBean(Class<T> ofType) {
    //noinspection unchecked
    return components.values().stream()
        .filter(c->ofType.isAssignableFrom(c.getClass()))
        .map(c->(T)c)
        .findAny();
  }

  @Override
  public <T> Map<String, T> getBeans(Class<T> ofType) {
    //noinspection unchecked
    return MapUtils.map(
        components.entrySet().stream()
        .filter(e->ofType.isAssignableFrom(e.getValue().getClass()))
            .map(e -> MapUtils.Pair.T(e.getKey(), (T) e.getValue()))
            .collect(Collectors.toSet())
    );
  }

  @Override
  public Map<String, Object> getBeans() {
    return Collections.unmodifiableMap(components);
  }
}
