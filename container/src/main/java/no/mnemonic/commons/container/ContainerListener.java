package no.mnemonic.commons.container;

/**
 *
 */
public interface ContainerListener {

  /**
   * Notify listeners that this container has been started
   * @param container which has started
   */
  void notifyContainerStarted(ComponentContainer container);

  /**
   * Notify listeners that this container will be destroyed
   * @param container which will be destroyed
   */
  void notifyContainerDestroying(ComponentContainer container);

  /**
   * Notify listeners that this container has been destroyed
   *
   * @param container which has been destroyed
   */
  void notifyContainerDestroyed(ComponentContainer container);

  /**
   * Notify listeners that a new container is about to initialize
   *
   * @param parent       reference to the calling container
   * @param subcontainer reference to the initializing subcontainer
   */
  void notifyInitializingSubcontainer(ComponentContainer parent, ComponentContainer subcontainer);

}
