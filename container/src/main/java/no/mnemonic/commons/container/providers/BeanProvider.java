package no.mnemonic.commons.container.providers;

import java.util.Map;
import java.util.Optional;

public interface BeanProvider {

  <T> Optional<T> getBean(Class<T> ofType);

  <T> Map<String, T> getBeans(Class<T> ofType);

  Map<String, Object> getBeans();

}
