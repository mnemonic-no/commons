package no.mnemonic.commons.container.providers;

import no.mnemonic.commons.component.ComponentException;
import no.mnemonic.commons.utilities.StreamUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.*;

public class SpringXmlBeanProvider implements BeanProvider {


  private final List<String> inputs;
  private final List<InputStream> inputStreams;
  private final Properties properties;
  private final ClassLoader classLoader;

  private ApplicationContext applicationContext;
  private BeanFactory parent;

  private SpringXmlBeanProvider(List<String> inputs, List<InputStream> inputStreams, Properties properties, ClassLoader classLoader) {
    this.inputs = inputs;
    this.inputStreams = inputStreams;
    this.properties = properties;
    this.classLoader = classLoader;
  }

  @Override
  public <T> Optional<T> getBean(Class<T> ofType) {
    try {
      return Optional.of(resolveBeans().getBean(ofType));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public <T> Map<String, T> getBeans(Class<T> ofType) {
    return resolveBeans().getBeansOfType(ofType);
  }

  @Override
  public Map<String, Object> getBeans() {
    return resolveBeans().getBeansOfType(Object.class);
  }

  private ApplicationContext resolveBeans() {

    if (applicationContext != null) return applicationContext;

    ApplicationContext parentCtx;
    if (parent == null) {
      parentCtx = null;
    } else if (parent instanceof ApplicationContext) {
      parentCtx = (ApplicationContext) parent;
    } else if (parent instanceof DefaultListableBeanFactory) {
      parentCtx = new GenericApplicationContext((DefaultListableBeanFactory) parent);
    } else {
      throw new RuntimeException("Cannot set up a new application context pointing to parent beanfactory of type " + parent.getClass());
    }

    //prepare factory
    List<Resource> res = fetchResources();
    GenericApplicationContext ctx = new GenericApplicationContext(parentCtx);

    // load resources
    for (Resource r : res) {
      XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
      reader.setBeanClassLoader(classLoader);
      reader.loadBeanDefinitions(r);
    }

    // parse properties
    if (properties != null) {
      PropertyPlaceholderConfigurer propCfg = new PropertyPlaceholderConfigurer();
      propCfg.setProperties(properties);
      ctx.addBeanFactoryPostProcessor(propCfg);
    }

    ctx.refresh();

    // instansiate singletons
    applicationContext = ctx;
    return applicationContext;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String toString() {
    return "[SpringXmlBeanProvider inputs=" + inputs + " inputStreams=" + inputStreams + " classLoader="
            + classLoader + " properties=" + properties + "]";
  }

  private List<Resource> fetchResources() throws ComponentException {
    try {
      //create resourcelist
      List<Resource> r = new ArrayList<>();
      //if the input string points to an existing file, use filesystemresource
      //else use classpathresource
      for (String s : inputs) {
        File f = new File(s);
        if (f.exists()) {
          r.add(new FileSystemResource(f));
        } else {
          r.add(new ClassPathResource(s));
        }
      }
      //attempt to load each configured inputstream
      for (InputStream is : inputStreams) {
        File f = File.createTempFile("XmlBeanProvider", "resource");
        OutputStream fos = new FileOutputStream(f);
        StreamUtils.writeUntilEOF(is, fos);
        fos.close();
        r.add(new FileSystemResource(f));
      }
      return r;
    } catch (IOException e) {
      throw new ComponentException(e);
    }
  }

  public static class Builder {

    private List<String> inputs = new ArrayList<>();
    private List<InputStream> inputStreams = new ArrayList<>();
    private Properties properties = System.getProperties();
    private ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    private Builder() {
    }

    public SpringXmlBeanProvider build() {
      return new SpringXmlBeanProvider(inputs, inputStreams, properties, classLoader);
    }

    public Builder addInput(String descriptorName) {
      this.inputs = ListUtils.addToList(this.inputs, descriptorName);
      return this;
    }

    public Builder addInputStream(InputStream is) {
      this.inputStreams = ListUtils.addToList(this.inputStreams, is);
      return this;
    }

    public Builder setInputs(List<String> inputs) {
      this.inputs = inputs;
      return this;
    }

    public Builder setInputStreams(List<InputStream> inputStreams) {
      this.inputStreams = inputStreams;
      return this;
    }

    public Builder setProperties(Properties properties) {
      this.properties = properties;
      return this;
    }

    public Builder setClassLoader(ClassLoader classLoader) {
      this.classLoader = classLoader;
      return this;
    }
  }
}
