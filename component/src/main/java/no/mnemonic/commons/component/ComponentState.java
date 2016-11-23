package no.mnemonic.commons.component;

public enum ComponentState {

  NOT_STARTED(false, false, false),
  INITIALIZING(true, false, false),
  STARTED(true, false, false),
  WARNING(true, false, false),
  STOPPING(true, true, false),
  STOPPED(false, true, true),
  FAILED(false, true, true);

  private boolean running;
  private boolean terminal;
  private boolean terminated;

  ComponentState(boolean running, boolean terminal, boolean terminated) {
    this.running = running;
    this.terminal = terminal;
    this.terminated = terminated;
  }

  public String getName() {
    return name();
  }

  public boolean isRunning() {
    return running;
  }

  public boolean isTerminal() {
    return terminal;
  }

  public boolean isStarted() {
    return this == STARTED || this == WARNING;
  }

  public boolean isTerminated() {
    return terminated;
  }

}
