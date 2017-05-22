package no.mnemonic.commons.container;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import static no.mnemonic.commons.container.BootStrap.APPLICATION_PROPERTIES_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class BootStrapTest {

  @Mock
  Consumer<ComponentContainer> containerStarted;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testBootStrapWithoutArguments() throws BootStrap.BootStrapException {
    new MyBootStrap().boot(new String[]{});
    verify(containerStarted, never()).accept(any());
  }

  @Test
  public void testBootStrapWithInvalidCommand() throws BootStrap.BootStrapException {
    try {
      new MyBootStrap().boot(new String[]{
              "invalid"
      });
      fail();
    } catch (BootStrap.BootStrapException e) {
      assertEquals(2, e.exitCode);
    }
  }

  @Test
  public void testBootStrap() throws BootStrap.BootStrapException, IOException {
    System.setProperty("propertyvalue", "1");
    new MyBootStrap().boot(new String[]{
            "guice",
            "module=" + MyModule.class.getName()
    });
    verify(containerStarted).accept(any());
  }

  @Test
  public void testBootStrapParsesPropertyFile() throws BootStrap.BootStrapException, IOException {
    File propsFile = createPropertyFile("prop", "propertyvalue", "1");
    System.setProperty(APPLICATION_PROPERTIES_FILE, propsFile.getAbsolutePath());
    ComponentContainer container = new MyBootStrap().boot(new String[]{
            "guice",
            "module=" + MyModule.class.getName()
    });
    MyComponent component = (MyComponent) container.getComponents().get(MyComponent.class.getSimpleName());
    assertEquals(1, component.myValue);
  }

  @Test(expected = BootStrap.BootStrapException.class)
  public void testBootStrapFailsOnUnknownIncludeFile() throws BootStrap.BootStrapException, IOException {
    File mainPropsFile = createPropertyFile("main", PropertiesResolver.INCLUDE_FILE_PREFIX + 1, "/tmp/bogus/file/path");
    System.setProperty(APPLICATION_PROPERTIES_FILE, mainPropsFile.getAbsolutePath());
    new BootStrapTest.MyBootStrap().boot(new String[]{
            "guice",
            "module=" + BootStrapTest.MyModule.class.getName()
    });
  }


  private File createPropertyFile(String id, String key, String value) throws IOException {
    Properties props = new Properties();
    props.put(key, value);
    File propsFile = File.createTempFile(getClass().getName(), "-" + id);
    props.store(new FileOutputStream(propsFile), "");
    return propsFile;
  }

  static class MyModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(MyComponent.class).in(Singleton.class);
    }
  }

  static class MyComponent {
    int myValue;

    @Inject
    public MyComponent(@Named("propertyvalue") int myValue) {
      this.myValue = myValue;
    }
  }

  class MyBootStrap extends BootStrap {
    @Override
    protected void containerStarted(ComponentContainer container) {
      containerStarted.accept(container);
    }
  }
}
