package no.mnemonic.commons.container.plugins;

import no.mnemonic.commons.component.ComponentNamingAspect;
import no.mnemonic.commons.container.ComponentContainer;
import no.mnemonic.commons.container.plugins.ComponentNamingPlugin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ComponentNamingPluginTest {

  @Mock
  private ComponentNamingAspect componentNamingAspect;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testAspectExecuted() {
    ComponentContainer container = ComponentContainer.create(componentNamingAspect, new ComponentNamingPlugin());
    container.initialize();
    verify(componentNamingAspect).setComponentName(any());
  }

  @Test
  public void testWithoutAspect() {
    ComponentContainer container = ComponentContainer.create(componentNamingAspect);
    container.initialize();
    verifyNoMoreInteractions(componentNamingAspect);
  }

  @Test
  public void testAspectWithNoTargets() {
    ComponentContainer container = ComponentContainer.create(new ComponentNamingPlugin());
    container.initialize();
    verifyNoMoreInteractions(componentNamingAspect);
  }

}
