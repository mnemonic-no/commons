package no.mnemonic.commons.component;

import java.util.Collection;

/**
 * This aspect makes the component management system provide this object a
 * with a reference to a {@link ComponentListener} object.
 * By contract, this object should notify the listener on relevant state changes,
 * see {@link ComponentListener} for definition of relevant state changes.
 *
 * Created by IntelliJ IDEA.
 * User: joakim
 * Date: 15.des.2004
 * Time: 14:10:36
 * Version: $Id$
 */
public interface ComponentListenerAspect {

  /**
   * Add this listener
   *
   * @param listener listener to component actions
   */
  void addComponentListener(ComponentListener listener);

  /**
   * @return all registered listeners
   */
  Collection<ComponentListener> getComponentListeners();

  /**
   * Remove this listener
   *
   * @param listener listener to remove
   */
  void removeComponentListener(ComponentListener listener);

}