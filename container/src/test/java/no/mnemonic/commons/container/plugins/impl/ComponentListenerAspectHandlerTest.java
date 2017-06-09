package no.mnemonic.commons.container.plugins.impl;

import no.mnemonic.commons.component.ComponentListenerAspect;
import no.mnemonic.commons.component.LifecycleAspect;
import no.mnemonic.commons.container.ComponentContainer;
import no.mnemonic.commons.container.ContainerListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;

public class ComponentListenerAspectHandlerTest {

  @Mock
  private ComponentListenerAspect listener;
  @Mock
  private LifecycleAspect component;

  @Before
  public void setup(){
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testStartContainerWithListener() {
    ComponentContainer.create(component, listener).initialize();
    InOrder ordered = inOrder(listener, component, listener);
    ordered.verify(listener).addComponentListener(isA(ComponentContainer.class));
    ordered.verify(component).startComponent();
  }

}
