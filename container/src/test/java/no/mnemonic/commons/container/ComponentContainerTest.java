package no.mnemonic.commons.container;

import no.mnemonic.commons.component.*;
import no.mnemonic.commons.container.providers.BeanProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static no.mnemonic.commons.utilities.collections.MapUtils.map;
import static no.mnemonic.commons.utilities.collections.MapUtils.pair;
import static org.mockito.Mockito.*;

public class ComponentContainerTest {

  @Mock
  private ValidationAspect validationAspect;
  @Mock
  private LifecycleAspect lifecycleAspect;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDeduplicateObjects() {
    ComponentContainer container = ComponentContainer.create(new BeanProvider() {
      @Override
      public <T> Optional<T> getBean(Class<T> ofType) {
        return Optional.empty();
      }

      @Override
      public <T> Map<String, T> getBeans(Class<T> ofType) {
        return map();
      }

      @Override
      public Map<String, Object> getBeans() {
        return map(
                pair("o1", lifecycleAspect),
                pair("o2", lifecycleAspect)
        );
      }
    });
    container.initialize();
    verify(lifecycleAspect, times(1)).startComponent();
  }

  @Test
  public void testInitializeValidatesComponents() {
    ComponentContainer container = ComponentContainer.create(validationAspect);
    verifyNoMoreInteractions(validationAspect);
    container.initialize();
    verify(validationAspect).validate(any());
  }

  @Test(expected = ComponentConfigurationException.class)
  public void testValidationErrorThrowsComponentInitializationException() {
    doAnswer(i -> ((ValidationContext) i.getArgument(0)).addError(validationAspect, "error"))
            .when(validationAspect).validate(any());
    ComponentContainer container = ComponentContainer.create(validationAspect);
    container.initialize();
  }

  @Test
  public void testInitializeStartsLifecycleComponents() {
    ComponentContainer container = ComponentContainer.create(lifecycleAspect);
    verifyNoMoreInteractions(lifecycleAspect);
    container.initialize();
    verify(lifecycleAspect).startComponent();
    verifyNoMoreInteractions(lifecycleAspect);
  }

  @Test(expected = ComponentException.class)
  public void testStartErrorThrowsComponentInitializationException() {
    doThrow(new RuntimeException("error during startup")).when(lifecycleAspect).startComponent();
    ComponentContainer container = ComponentContainer.create(lifecycleAspect);
    container.initialize();
  }

  @Test
  public void testDestroyStopsLifecycleComponents() {
    ComponentContainer container = ComponentContainer.create(lifecycleAspect);
    container.initialize();
    container.destroy();
    verify(lifecycleAspect).stopComponent();
  }

  @Test
  public void testErrorDuringComponentShutdown() {
    doThrow(new RuntimeException("error during startup")).when(lifecycleAspect).stopComponent();
    ComponentContainer container = ComponentContainer.create(lifecycleAspect);
    container.initialize();
    container.destroy();
  }

  @Test
  public void testDependencyByGetterAnnotationDuringStartup() {
    Consumer<Object> startConsumer = mock(Consumer.class);
    Consumer<Object> stopConsumer = mock(Consumer.class);
    ComponentA a = new ComponentA();
    ComponentAProvider ap = new ComponentAProvider(a, startConsumer, stopConsumer);
    ComponentB b = new ComponentB(startConsumer, stopConsumer, a);
    ComponentC c = new ComponentC(startConsumer, stopConsumer, a, b);
    ComponentD d = new ComponentD(startConsumer, stopConsumer, a, b, c);
    InOrder order = inOrder(startConsumer);
    ComponentContainer container = ComponentContainer.create(ap, a, b, c, d);
    container.initialize();
    order.verify(startConsumer).accept(ap);
    order.verify(startConsumer).accept(b);
    order.verify(startConsumer).accept(c);
    order.verify(startConsumer).accept(d);
  }

  @Test
  public void testDependencyByGetterAnnotationDuringShutdown() {
    Consumer<Object> startConsumer = mock(Consumer.class);
    Consumer<Object> stopConsumer = mock(Consumer.class);
    ComponentA a = new ComponentA();
    ComponentAProvider ap = new ComponentAProvider(a, startConsumer, stopConsumer);
    ComponentB b = new ComponentB(startConsumer, stopConsumer, a);
    ComponentC c = new ComponentC(startConsumer, stopConsumer, a, b);
    ComponentD d = new ComponentD(startConsumer, stopConsumer, a, b, c);
    InOrder order = inOrder(stopConsumer);
    ComponentContainer container = ComponentContainer.create(ap, a, b, c, d);
    container.initialize();
    container.destroy();
    order.verify(stopConsumer).accept(d);
    order.verify(stopConsumer).accept(c);
    order.verify(stopConsumer).accept(b);
    order.verify(stopConsumer).accept(ap);
  }

  public abstract class TestClass implements LifecycleAspect {
    private Consumer<Object> startConsumer;
    private Consumer<Object> stopConsumer;

    public TestClass(Consumer<Object> startConsumer, Consumer<Object> stopConsumer) {
      this.startConsumer = startConsumer;
      this.stopConsumer = stopConsumer;
    }

    @Override
    public String toString() {
      return getClass().getName();
    }

    @Override
    public void startComponent() {
      startConsumer.accept(this);
    }

    @Override
    public void stopComponent() {
      stopConsumer.accept(this);
    }
  }

  public class ComponentA {
    public ComponentA() {
      super();
    }
  }

  public class ComponentAProvider implements DependencyProvider, LifecycleAspect {

    private final ComponentA componentA;
    private final Consumer<Object> startConsumer;
    private final Consumer<Object> stopConsumer;

    public ComponentAProvider(ComponentA componentA, Consumer<Object> startConsumer, Consumer<Object> stopConsumer) {
      this.componentA = componentA;
      this.startConsumer = startConsumer;
      this.stopConsumer = stopConsumer;
    }

    @Override
    public Object getProvidedDependency() {
      return componentA;
    }

    @Override
    public void startComponent() {
      startConsumer.accept(this);
    }

    @Override
    public void stopComponent() {
      stopConsumer.accept(this);
    }
  }

  public class ComponentB extends TestClass {
    ComponentA componentA;

    public ComponentB(Consumer<Object> startConsumer, Consumer<Object> stopConsumer, ComponentA componentA) {
      super(startConsumer, stopConsumer);
      this.componentA = componentA;
    }

    @Dependency
    public ComponentA getComponentA() {
      return componentA;
    }
  }

  public class ComponentC extends TestClass {
    ComponentA componentA;
    ComponentB componentB;

    public ComponentC(Consumer<Object> startConsumer, Consumer<Object> stopConsumer, ComponentA componentA, ComponentB componentB) {
      super(startConsumer, stopConsumer);
      this.componentA = componentA;
      this.componentB = componentB;
    }

    @Dependency
    public ComponentA getComponentA() {
      return componentA;
    }

    @Dependency
    public ComponentB getComponentB() {
      return componentB;
    }

  }

  public class ComponentD extends TestClass {
    @Dependency
    ComponentA componentA;
    @Dependency
    ComponentB componentB;
    @Dependency
    ComponentC componentC;

    public ComponentD(Consumer<Object> startConsumer, Consumer<Object> stopConsumer, ComponentA componentA, ComponentB componentB, ComponentC componentC) {
      super(startConsumer, stopConsumer);
      this.componentA = componentA;
      this.componentB = componentB;
      this.componentC = componentC;
    }
  }
}
