package no.mnemonic.commons.container.providers;

import com.google.inject.*;
import com.google.inject.name.Names;
import no.mnemonic.commons.utilities.collections.SetUtils;

import java.util.*;

public class GuiceBeanProvider implements BeanProvider{

  private final Injector injector;

  public GuiceBeanProvider(Module module, Properties properties) {
    Set<Module> modules = SetUtils.set(module);
    if (properties != null) {
      modules.add(createPropertiesModule(properties));
    }
    injector = Guice.createInjector(modules);
  }

  @Override
  public <T> Optional<T> getBean(Class<T> ofType) {
    try {
      return Optional.of(injector.getProvider(ofType).get());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public <T> Map<String, T> getBeans(Class<T> ofType) {
    Map<String, T> result = new HashMap<>();
    for (Binding<T> t : injector.findBindingsByType(TypeLiteral.get(ofType))) {
      result.put(t.getKey().toString(), t.getProvider().get());
    }
    return result;
  }

  @Override
  public Map<String, Object> getBeans() {
    Map<String, Object> result = new HashMap<>();
    for (Binding<?> t : injector.getAllBindings().values()) {
      result.put(t.getKey().toString(), t.getProvider().get());
    }
    return result;
  }

  private Module createPropertiesModule(Properties properties) {
    return new AbstractModule() {
      @Override
      protected void configure() {
        Names.bindProperties(binder(), properties);
      }
    };
  }
}
