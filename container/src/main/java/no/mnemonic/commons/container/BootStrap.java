package no.mnemonic.commons.container;

import com.google.inject.Module;
import no.mnemonic.commons.component.Component;
import no.mnemonic.commons.component.ComponentListener;
import no.mnemonic.commons.component.Versioned;
import no.mnemonic.commons.container.providers.BeanProvider;
import no.mnemonic.commons.container.providers.GuiceBeanProvider;
import no.mnemonic.commons.container.providers.SpringXmlBeanProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
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

  private static final String APPLICATION_PROPERTIES_FILE = "application.properties.file";
  private static final int EXIT_CODE_ARGUMENT_ERROR = 2;
  private static final int EXIT_CODE_EXEC_ERROR = 1;
  private static final String PROP_GUICE = "guice";
  private static final String PROP_SPRING = "spring";

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

  //public methods

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    // Start the application.
    new BootStrap().boot(args);
  }

  //private methods

  private BootStrap() {
  }

  private void title() {
    System.out.println("******************************************");
    System.out.println("Bootstrap " + getPackageVersion());
    System.out.println("******************************************");
  }

  private static void usage() {
    System.out.println("Usage:");
    System.out.println("   java " + BootStrap.class.getName() + " spring=<xmlResource> [param1=value [param2=value2 ...]]");
    System.out.println("   java " + BootStrap.class.getName() + " guice=<moduleClass> [param1=value [param2=value2 ...]]");
    System.out.println("  xmlResource - may be a classpath resource or a path to a file");
    System.out.println("  moduleClass - full qualified class name to a Guice resource");
    System.out.println();
    System.out.println("Parameters except for guice and spring directive are passed on to the created container as properties.");
  }

  private ComponentContainer boot(Properties namingParameters, Properties properties) {
    BeanProvider beanProvider;
    if (namingParameters.containsKey(PROP_SPRING)) {
      beanProvider = getSpringBootContainer(namingParameters.getProperty(PROP_SPRING), properties);
    } else if (namingParameters.containsKey(PROP_GUICE)) {
      beanProvider = getGuiceBootContainer(namingParameters.getProperty(PROP_GUICE), properties);
    } else {
      throw new RuntimeException("Unknown boot type (use spring or guice");
    }
    //create the administration container housing the boot components
    ComponentContainer bootContainer = ComponentContainer.create(beanProvider);
    bootContainer.addComponentListener(this);
    //start the boot components
    bootContainer.initialize();
    return bootContainer;

  }

  private ComponentContainer boot(String[] args) {
    title();

    try {
      if (args.length > 0) {
        Properties namingParameters = new Properties();
        Pattern p = Pattern.compile("([0-9a-zA-Z]+)=([^ ]+)");
        for (String arg : args) {
          Matcher m = p.matcher(arg);
          if (!m.matches()) {
            System.out.println("Malformed argument: " + arg);
            usage();
            System.exit(EXIT_CODE_ARGUMENT_ERROR);
          }
          String key = m.group(1);
          String value = m.group(2);
          namingParameters.put(key, value);
        }

        return boot(namingParameters, resolveProperties());
      } else {
        usage();
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(EXIT_CODE_EXEC_ERROR);
      return null;
    }
  }

  private Properties resolveProperties() {
    String propertyFileName = System.getProperty(APPLICATION_PROPERTIES_FILE);
    try {
      Properties p = System.getProperties();
      if (propertyFileName != null) {
        p = new Properties(p);
        p.load(new FileInputStream(propertyFileName));
      }
      return p;
    } catch (IOException e) {
      throw new RuntimeException("Could not load property file: " + propertyFileName);
    }
  }

  private BeanProvider getSpringBootContainer(String bootDescriptorName, Properties properties) {
    return SpringXmlBeanProvider.builder()
        .addInput(bootDescriptorName)
        .setProperties(properties)
        .build();
  }

  private BeanProvider getGuiceBootContainer(String bootDescriptorName, Properties properties) {
    try {
      Module moduleClass = (Module) Class.forName(bootDescriptorName).newInstance();
      return new GuiceBeanProvider(moduleClass, properties);
    } catch (Exception e) {
      throw new RuntimeException("Invalid Guice module: " + bootDescriptorName);
    }
  }
}