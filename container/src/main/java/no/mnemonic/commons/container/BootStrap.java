package no.mnemonic.commons.container;

import com.google.inject.Module;
import no.mnemonic.commons.component.Component;
import no.mnemonic.commons.component.ComponentListener;
import no.mnemonic.commons.component.Versioned;
import no.mnemonic.commons.container.providers.BeanProvider;
import no.mnemonic.commons.container.providers.GuiceBeanProvider;
import no.mnemonic.commons.container.providers.SpringXmlBeanProvider;
import no.mnemonic.commons.utilities.collections.CollectionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bootstraps a collection of components. <p>Uses an initial bootstrap configuration file that may include all the
 * components or, optionally, a reference to the actual source of configuration information.
 * <p>
 * Usage:
 * <ol>
 * <li>Optionally, create and initialize a BeanCollection</li>
 * <li>Create a Bootstrap instance, optionally passing the BeanCollection from the previous step</li>
 * <li>Call Bootstrap.init()</li>
 * <li>Call Bootstrap.getBeanCollection(), and get the components you need from it.</li>
 * </ol>
 */
public class BootStrap implements Versioned, ComponentListener {

  static final String APPLICATION_PROPERTIES_FILE = "application.properties.file";
  private static final int EXIT_CODE_ARGUMENT_ERROR = 2;
  private static final int EXIT_CODE_EXEC_ERROR = 1;
  private static final String PROP_GUICE = "guice";
  private static final String PROP_SPRING = "spring";
  private static final String PROP_MODULE = "module";

  //interface methods

  @Override
  public String getPackageVersion() {
    if (getClass().getPackage() == null) return null;
    return getClass().getPackage().getImplementationVersion();
  }

  @Override
  public void notifyComponentStopped(Component component) {
  }

  @Override
  public void notifyComponentStopping(Component component) {
  }

  protected void containerStarted(ComponentContainer container) {
  }

  //public methods

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    // Start the application.
    try {
      new BootStrap().boot(args);
    } catch (BootStrapException e) {
      System.exit(e.exitCode);
    }
  }

  //private methods

  //allow subclasses
  @SuppressWarnings("WeakerAccess")
  protected BootStrap() {
  }

  private void title() {
    System.out.println("******************************************");
    System.out.println("Bootstrap " + getPackageVersion());
    System.out.println("******************************************");
  }

  private static void usage() {
    System.out.println("Usage:");
    System.out.println("   java " + BootStrap.class.getName() + " spring <xmlResource> [param1=value [param2=value2 ...]]");
    System.out.println("   java " + BootStrap.class.getName() + " guice module=<moduleClass> [module=<moduleClass> ...] [param1=value [param2=value2 ...]]");
    System.out.println("  xmlResource - may be a classpath resource or a path to a file");
    System.out.println("  moduleClass - full qualified class name to a Guice resource");
    System.out.println();
    System.out.println("Parameters except for guice and spring directive are passed on to the created container as properties.");
  }

  //allow subclasses
  @SuppressWarnings("WeakerAccess")
  protected ComponentContainer boot(String[] args) throws BootStrapException {
    title();

    try {
      if (args.length == 0) {
        usage();
        return null;
      }

      switch (args[0]) {
        case PROP_SPRING:
          return springBoot(args[1], Arrays.copyOfRange(args, 2, args.length));
        case PROP_GUICE:
          return guiceBoot(Arrays.copyOfRange(args, 1, args.length));
        default:
          usage();
          throw new BootStrapException(null, EXIT_CODE_ARGUMENT_ERROR);
      }
    } catch (BootStrapException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new BootStrapException(e, EXIT_CODE_EXEC_ERROR);
    }
  }

  class BootStrapException extends Exception {
    final int exitCode;

    BootStrapException(Throwable cause, int exitCode) {
      super(cause);
      this.exitCode = exitCode;
    }
  }

  private static class Property {
    private final String key, value;

    Property(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  private Property parseProperty(String arg) {
    Pattern p = Pattern.compile("([0-9a-zA-Z]+)=([^ ]+)");
    Matcher m = p.matcher(arg);
    if (!m.matches()) {
      System.err.println("Malformed argument: " + arg);
      usage();
      System.exit(EXIT_CODE_ARGUMENT_ERROR);
    }
    return new Property(m.group(1), m.group(2));
  }

  private ComponentContainer guiceBoot(String[] remainingArgs) {
    Set<String> moduleClasses = new HashSet<>();
    //noinspection MismatchedQueryAndUpdateOfCollection
    Properties containerProps = new Properties();
    for (String arg : remainingArgs) {
      Property p = parseProperty(arg);
      if (p.key.equals(PROP_MODULE)) {
        moduleClasses.add(p.value);
      } else {
        containerProps.setProperty(p.key, p.value);
      }
    }
    return bootContainer(getGuiceBootContainer(moduleClasses));
  }

  private ComponentContainer springBoot(String springResource, String[] containerParameters) {
    //container props not used for now, keep for later
    //noinspection MismatchedQueryAndUpdateOfCollection
    Properties containerProps = new Properties();
    for (String s : containerParameters) {
      Property p = parseProperty(s);
      containerProps.setProperty(p.key, p.value);
    }
    return bootContainer(getSpringBootContainer(springResource));
  }

  private ComponentContainer bootContainer(BeanProvider beanProvider) {
    ComponentContainer bootContainer = ComponentContainer.create(beanProvider);
    bootContainer.addComponentListener(this);
    //start the boot components
    bootContainer.initialize();
    //allow subclasses to add functionality here
    containerStarted(bootContainer);
    return bootContainer;
  }

  private static Properties resolveProperties() {
    String propertyFileName = System.getProperty(APPLICATION_PROPERTIES_FILE);
    Properties properties = new Properties(System.getProperties());
    if (propertyFileName != null) {
      PropertiesResolver.loadPropertiesFile(new File(propertyFileName), properties);
    }
    return properties;
  }

  private BeanProvider getSpringBootContainer(String bootDescriptorName) {
    return SpringXmlBeanProvider.builder()
            .addInput(bootDescriptorName)
            .setProperties(resolveProperties())
            .build();
  }

  private BeanProvider getGuiceBootContainer(Set<String> moduleClasses) {
    if (CollectionUtils.isEmpty(moduleClasses)) {
      throw new IllegalArgumentException("No modules specified");
    }
    try {
      Set<Module> modules = new HashSet<>();
      for (String clz : moduleClasses) {
        modules.add((Module) Class.forName(clz).newInstance());
      }
      return new GuiceBeanProvider(resolveProperties(), modules.toArray(new Module[]{}));
    } catch (Exception e) {
      throw new RuntimeException("Error booting modules", e);
    }
  }
}