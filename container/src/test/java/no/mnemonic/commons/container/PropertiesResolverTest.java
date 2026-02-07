package no.mnemonic.commons.container;

import no.mnemonic.commons.utilities.collections.MapUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static no.mnemonic.commons.container.BootStrap.APPLICATION_PROPERTIES_FILE;
import static no.mnemonic.commons.container.PropertiesResolver.DEFAULTS_FILE_PREFIX;
import static no.mnemonic.commons.container.PropertiesResolver.INCLUDE_FILE_PREFIX;
import static no.mnemonic.commons.utilities.collections.MapUtils.map;
import static no.mnemonic.commons.utilities.collections.MapUtils.pair;
import static org.junit.Assert.assertEquals;

public class PropertiesResolverTest {

  @Test
  public void testResolveProperties() throws IOException {
    File mainPropsFile = createPropertyFile("main", "propertyvalue", "1");
    assertEquals("1", PropertiesResolver.loadPropertiesFile(mainPropsFile).getProperty("propertyvalue"));
  }

  @Test
  public void testResolvePropertiesWithEnvvars() throws IOException {
    String user = System.getenv("USER");
    File mainPropsFile = createPropertyFile("main", "propertyvalue", "1");
    assertEquals(user, PropertiesResolver.loadPropertiesFile(mainPropsFile, true, false).getProperty("USER"));
  }

  @Test
  public void testResolvePropertiesWithSystemProps() throws IOException {
    System.setProperty("myprop", "propvalue");
    File mainPropsFile = createPropertyFile("main", "propertyvalue", "1");
    assertEquals("propvalue", PropertiesResolver.loadPropertiesFile(mainPropsFile, false, true).getProperty("myprop"));
  }

  @Test
  public void testResolvePropertiesWithoutSystemProps() throws IOException {
    String user = System.getenv("USER");
    System.setProperty("USER", "someuser");
    File mainPropsFile = createPropertyFile("main", "propertyvalue", "1");
    assertEquals(user, PropertiesResolver.loadPropertiesFile(mainPropsFile, true, false).getProperty("USER"));
  }


  @Test
  public void testResolveSystemPropertiesOverridesEnvvars() throws IOException {
    System.setProperty("USER", "someuser");
    File mainPropsFile = createPropertyFile("main", "propertyvalue", "1");
    assertEquals("someuser", PropertiesResolver.loadPropertiesFile(mainPropsFile, true, true).getProperty("USER"));
  }

  @Test
  public void testResolveIncludedProperties() throws IOException {
    File includedPropsFile = createPropertyFile("include", "propertyvalue", "1");
    File mainPropsFile = createPropertyFile("main", INCLUDE_FILE_PREFIX + 1, includedPropsFile.getAbsolutePath());
    assertEquals("1", PropertiesResolver.loadPropertiesFile(mainPropsFile).getProperty("propertyvalue"));
  }

  @Test
  public void testBootStrapParsesRecursivePropertyFile() throws IOException {
    File leafPropertyFile = createPropertyFile("leaf", "propertyvalue", "1");
    File middlePropertyFile = createPropertyFile("middle", INCLUDE_FILE_PREFIX + 1, leafPropertyFile.getAbsolutePath());
    File mainPropsFile = createPropertyFile("main", INCLUDE_FILE_PREFIX + 1, middlePropertyFile.getAbsolutePath());
    assertEquals("1",PropertiesResolver.loadPropertiesFile(mainPropsFile).getProperty("propertyvalue"));
  }

  @Test
  public void testBootStrapDuplicateInclude() throws IOException {
    File leafPropertyFile = createPropertyFile("leaf", "propertyvalue", "1");
    File middlePropertyFile1 = createPropertyFile("middle1", map(
            pair(INCLUDE_FILE_PREFIX + 1, leafPropertyFile.getAbsolutePath()),
            pair("a", "b")
    ));
    File middlePropertyFile2 = createPropertyFile("middle2", map(
            pair(INCLUDE_FILE_PREFIX + 1, leafPropertyFile.getAbsolutePath()),
            pair("x", "y")
    ));
    File mainPropsFile = createPropertyFile("main", map(
            pair(INCLUDE_FILE_PREFIX + 1, middlePropertyFile1.getAbsolutePath()),
            pair(INCLUDE_FILE_PREFIX + 2, middlePropertyFile2.getAbsolutePath())
    ));
    System.setProperty(APPLICATION_PROPERTIES_FILE, mainPropsFile.getAbsolutePath());
    Properties props =PropertiesResolver.loadPropertiesFile(mainPropsFile);
    assertEquals("1", props.getProperty("propertyvalue"));
    assertEquals("b", props.getProperty("a"));
    assertEquals("y", props.getProperty("x"));
  }

  @Test
  public void testBootStrapIncludeValuesOverride() throws IOException {
    File leafPropertyFile = createPropertyFile("leaf", "propertyvalue", "leaf");
    File mainPropsFile = createPropertyFile("main", map(
            pair(INCLUDE_FILE_PREFIX + 1, leafPropertyFile.getAbsolutePath()),
            pair("propertyvalue", "main")
    ));
    System.setProperty(APPLICATION_PROPERTIES_FILE, mainPropsFile.getAbsolutePath());
    Properties props =PropertiesResolver.loadPropertiesFile(mainPropsFile);
    assertEquals("leaf", props.getProperty("propertyvalue"));
  }

  @Test
  public void testBootStrapDefaultValues() throws IOException {
    File leafPropertyFile = createPropertyFile("leaf", "propertyvalue", "leaf");
    File mainPropsFile = createPropertyFile("main", map(
            pair(DEFAULTS_FILE_PREFIX + 1, leafPropertyFile.getAbsolutePath()),
            pair("propertyvalue", "main")
    ));
    System.setProperty(APPLICATION_PROPERTIES_FILE, mainPropsFile.getAbsolutePath());
    Properties props =PropertiesResolver.loadPropertiesFile(mainPropsFile);
    assertEquals("main", props.getProperty("propertyvalue"));
  }

  private File createPropertyFile(String id, String key, String value) throws IOException {
    return createPropertyFile(id, map(MapUtils.Pair.T(key, value)));
  }

  private File createPropertyFile(String id, Map<String, String> entries) throws IOException {
    Properties props = new Properties();
    map(entries).forEach(props::setProperty);
    File propsFile = File.createTempFile(getClass().getName(), "-" + id);
    props.store(new FileOutputStream(propsFile), "");
    return propsFile;
  }

}
