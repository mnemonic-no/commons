package no.mnemonic.commons.container.plugins;

/**
 * Use this interface to implement lifecycle management on your components.
 * Each handler implementation can apply to the objects it selects, and the handler will be called to start
 * all components it accepts on container startup, and to stop on container shutdown.
 *
 * If multiple lifecycle handlers accept the same objects, the container does not define which one of the handlers will be invoked.
 */
public interface ComponentLifecycleHandler {

  boolean appliesTo(Object obj);

  void startComponent(Object obj);

  void stopComponent(Object obj);

}
