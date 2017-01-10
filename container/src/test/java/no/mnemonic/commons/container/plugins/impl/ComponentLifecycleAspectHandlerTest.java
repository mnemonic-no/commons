package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.container.plugins.impl.ComponentLifecycleAspectHandler;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ComponentLifecycleAspectHandlerTest {

  @Test
  public void testAppliesTo() {
    ComponentLifecycleAspectHandler handler = new ComponentLifecycleAspectHandler();
    assertFalse(handler.appliesTo(null));
    assertFalse(handler.appliesTo("string"));
    assertFalse(handler.appliesTo(1));
    assertTrue(handler.appliesTo(new LifecycleAspect() {
      @Override
      public void startComponent() {
      }

      @Override
      public void stopComponent() {
      }
    }));
  }

  @Test
  public void startNull() {
    new ComponentLifecycleAspectHandler().startComponent(null);
  }

  @Test
  public void stopNull() {
    new ComponentLifecycleAspectHandler().stopComponent(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void startWrongClass() {
    new ComponentLifecycleAspectHandler().startComponent("string");
  }

  @Test(expected = IllegalArgumentException.class)
  public void stopWrongClass() {
    new ComponentLifecycleAspectHandler().stopComponent("string");
  }

  @Test
  public void startComponent() {
    LifecycleAspect l = mock(LifecycleAspect.class);
    new ComponentLifecycleAspectHandler().startComponent(l);
    verify(l).startComponent();
  }

  @Test
  public void stopComponent() {
    LifecycleAspect l = mock(LifecycleAspect.class);
    new ComponentLifecycleAspectHandler().stopComponent(l);
    verify(l).stopComponent();
  }

  @Test(expected = RuntimeException.class)
  public void stopComponentWithError() {
    LifecycleAspect l = mock(LifecycleAspect.class);
    doThrow(new RuntimeException()).when(l).stopComponent();
    new ComponentLifecycleAspectHandler().stopComponent(l);
  }

  @Test(expected = RuntimeException.class)
  public void startComponentWithError() {
    LifecycleAspect l = mock(LifecycleAspect.class);
    doThrow(new RuntimeException()).when(l).startComponent();
    new ComponentLifecycleAspectHandler().startComponent(l);
  }
}
