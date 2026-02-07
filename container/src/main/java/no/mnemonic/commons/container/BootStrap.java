package no.mnemonic.commons.container;

import com.google.inject.Module;
import no.mnemonic.commons.component.ComponentListener;
import no.mnemonic.commons.component.Versioned;
import no.mnemonic.commons.container.providers.BeanProvider;
import no.mnemonic.commons.container.providers.GuiceBeanProvider;
import no.mnemonic.commons.container.providers.SpringXmlBeanProvider;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.SetUtils;

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
 *
 * Usage: BootStrap [-E] guice module=module1 module=module2 ...
 *
 * <ol>
 * <li>Use -E option to enable use of envvars</li>
 * <li>Create a Bootstrap instance, optionally passing the BeanCollection from the previous step</li>
 * <li>Call Bootstrap.init()</li>
 * <li>Call Bootstrap.getBeanCollection(), and get the components you need from it.</li>
 * </ol>
 * <p>
 * See {@link PropertiesResolver} for details on resolving of properties.
 * </p>
 */
public class BootStrap implements Versioned, ComponentListener {

  static final String APPLICATION_PROPERTIES_FILE = "application.properties.file";
  private static final int EXIT_CODE_ARGUMENT_ERROR = 2;
  private static final int EXIT_CODE_EXEC_ERROR = 1;
  private static final String PROP_GUICE = "guice";
  private static final String PROP_SPRING = "spring";
  private static final String PROP_MODULE = "module";
  private static final String SWITCH_ENV = "-E";

  //interface methods

  @Override
  public String getPackageVersion() {
    if (getClass().getPackage() == null) return null;
    return getClass().getPackage().getImplementationVersion();
  }

  @Override
  public void notifyComponentStopped(Object component) {
  }

  @Override
  public void notifyComponentStopping(Object component) {
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

      int nextarg = 0;
      boolean useEnv = false;

      //extract flags
      while (nextarg < args.length && !SetUtils.in(args[nextarg], PROP_SPRING, PROP_GUICE)) {
        switch (args[nextarg]) {
          case SWITCH_ENV:
            useEnv = true;
            break;
          default:
            usage();
            throw new BootStrapException(null, EXIT_CODE_ARGUMENT_ERROR);
        }
        nextarg++;
      }

      args = Arrays.copyOfRange(args, nextarg, args.length);

      if (!SetUtils.in(args[0], PROP_SPRING, PROP_GUICE)) {
        usage();
        throw new BootStrapException(null, EXIT_CODE_ARGUMENT_ERROR);
      }

      switch (args[0]) {
        case PROP_SPRING:
          return springBoot(args[1], useEnv, Arrays.copyOfRange(args, 2, args.length));
        case PROP_GUICE:
          return guiceBoot(useEnv, Arrays.copyOfRange(args, 1, args.length));
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

  private ComponentContainer guiceBoot(boolean useEnv, String[] remainingArgs) {
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
    return bootContainer(getGuiceBootContainer(useEnv, moduleClasses));
  }

  private ComponentContainer springBoot(String springResource, boolean useEnv, String[] containerParameters) {
    //container props not used for now, keep for later
    //noinspection MismatchedQueryAndUpdateOfCollection
    Properties containerProps = new Properties();
    for (String s : containerParameters) {
      Property p = parseProperty(s);
      containerProps.setProperty(p.key, p.value);
    }
    return bootContainer(getSpringBootContainer(useEnv, springResource));
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

  private static Properties resolveProperties(boolean useEnv) {
    String propertyFileName = System.getProperty(APPLICATION_PROPERTIES_FILE);

    if (propertyFileName != null) {
      return PropertiesResolver.loadPropertiesFile(new File(propertyFileName), useEnv, true);
    } else {
      Properties properties = new Properties();
      if (useEnv) properties.putAll(System.getenv());
      properties.putAll(System.getProperties());
      return properties;
    }
  }

  private BeanProvider getSpringBootContainer(boolean useEnv, String bootDescriptorName) {
    return SpringXmlBeanProvider.builder()
            .addInput(bootDescriptorName)
            .setProperties(resolveProperties(useEnv))
            .build();
  }

  private BeanProvider getGuiceBootContainer(boolean useEnv, Set<String> moduleClasses) {
    if (CollectionUtils.isEmpty(moduleClasses)) {
      throw new IllegalArgumentException("No modules specified");
    }
    try {
      Set<Module> modules = new HashSet<>();
      for (String clz : moduleClasses) {
        modules.add((Module) Class.forName(clz).newInstance());
      }
      return new GuiceBeanProvider(resolveProperties(useEnv), modules.toArray(new Module[]{}));
    } catch (Exception e) {
      throw new RuntimeException("Error booting modules", e);
    }
  }
}