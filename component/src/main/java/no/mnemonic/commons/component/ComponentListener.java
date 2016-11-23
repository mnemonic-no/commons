package no.mnemonic.commons.component;

/**
 * ComponentListener.java:
 *
 * This aspect causes this object to be notified on state changes
 * in other components. A component implementing
 * {@link ComponentListenerAspect} is given reference to this ComponentListener,
 * and by contract, should call this ComponentListener upon relevant state changes
 * (currently, only on component termination).
 *
 * Used for making active components give feedback to the administrative system upon important state changes.
 *
 * @author joakim
 *         Date: 08.des.2004
 *         Time: 16:30:16
 * @version $Id$
 */
public interface ComponentListener {


  /**
   * The component given this listener, should call this method
   * upon termination of the component.
   *
   * @param component reference to the calling component itself, to give the
   *                  listener an idea which component is terminating.
   */
  void notifyComponentStopped(Component component);

  /**
   * Signal whoever cares that this component is actively attempting to cleanly shut down
   *
   * @param component reference to the calling component itself, to give the listener
   *                  an idea that the component is in the process of shutting down
   */
  void notifyComponentStopping(Component component);
}
