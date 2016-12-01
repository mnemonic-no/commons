package no.mnemonic.commons.container.exceptions;

import no.mnemonic.commons.component.ComponentIdentity;

public class NoSuchComponentException extends RuntimeException {

  static final long serialVersionUID = 72834000135032557L;

  public NoSuchComponentException(ComponentIdentity identity) {
    super("No such component: " + identity);
  }

  public NoSuchComponentException(ComponentIdentity identity, String type) {
    super("No such component: " + identity + " of type " + type);
  }

  public NoSuchComponentException(String identity) {
    super("No such component: " + identity);
  }
}

