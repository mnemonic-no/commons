package no.mnemonic.commons.container;

import no.mnemonic.commons.logging.Logger;
import no.mnemonic.commons.logging.Logging;
import no.mnemonic.commons.utilities.collections.MapUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Resolve properties from a configuration file.
 *
 * The resolver supports including properties from sub-files, using
 * <ul>
 *   <li><code>include.file.1=filename</code> - Include this file (in order of index). Duplicate values prioritizes the included file</li>
 *   <li><code>defaults.file.1=filename</code> - Include this file (in order of index). Duplicate values prioritizes already resolved properties</li>
 * </ul>
 *
 * Multiple files can be included by
 * <code>
 *   include.file.1=file1
 *   include.file.2=file2
 * </code>
 *
 * Use <code>include.file</code> to load overrides from a subfile (defaulting to local values).
 * Use <code>defaults.file</code> to load defaults from a subfile (overriding with local values)
 *
 * Loading with envvars and system properties is optional (default disabled).
 * Envvars and system properties take precedence over properties loaded from file.
 * System properties take precedence over envvars.
 *
 * Nested loading is supported.
 */
public class PropertiesResolver {

  static final String INCLUDE_FILE_PREFIX = "include.file.";
  static final String DEFAULTS_FILE_PREFIX = "defaults.file.";

  private static final Logger LOGGER = Logging.getLogger(PropertiesResolver.class);

  public static Properties loadPropertiesFile(File file) {
    return loadPropertiesFile(file, false, false);
  }

  /**
   * Load properties from given file, with recursive resolving of includes/defaults
   * @param file properties file to load
   * @param useEnvVars Include envvars in result. Envvars take precedence over loaded properties
   * @param useSystemProperties Include system properties in result. System properties take precedence over loaded properties AND envvars.
   * @return the loaded properties
   */
  public static Properties loadPropertiesFile(File file, boolean useEnvVars, boolean useSystemProperties) {
    Set<File> loadedFiles = new HashSet<>();
    Properties properties = new Properties();

    //now load file
    properties = loadPropertiesFile(file, loadedFiles);

    //re-write envvars and system properties to make sure these take presendence
    if (useEnvVars) properties.putAll(System.getenv());
    //overwrite (system properties take precedence over envvars)
    if (useSystemProperties) properties.putAll(System.getProperties());

    return properties;
  }

  /**
   *
   * @param file file to load
   * @param loadedFiles set of already loaded files, to avoid infinite loops
   * @return the properties loaded from the file (recursively)
   */
  private static Properties loadPropertiesFile(File file, Set<File> loadedFiles) {
    if (loadedFiles.contains(file)) {
      LOGGER.info("Skipping previously loaded file: " + file);
      return new Properties();
    }
    LOGGER.info("Loading properties from file: " + file);
    Properties newProps = new Properties();
    try (InputStream is = new FileInputStream(file)) {
      newProps.load(is);
    } catch (IOException e) {
      throw new RuntimeException("Could not load property file: " + file);
    }
    loadedFiles.add(file);

    //resolve defaults from referenced default files
    Properties properties = resolvePropertiesFromIncludeFiles(newProps, loadedFiles, DEFAULTS_FILE_PREFIX);
    //overwrite with properties loaded from this file
    properties.putAll(newProps);
    //overwrite with properties resolved from included files
    properties.putAll(resolvePropertiesFromIncludeFiles(newProps, loadedFiles, INCLUDE_FILE_PREFIX));

    return properties;
  }

  private static Properties resolvePropertiesFromIncludeFiles(Properties properties, Set<File> loadedFiles, String includeFilePrefix) {
    Properties result = new Properties();
    MapUtils.map(properties)
            .entrySet()
            .stream()
            .filter(e -> String.valueOf(e.getKey()).startsWith(includeFilePrefix))
            .map(e -> new File(String.valueOf(e.getValue())))
            .forEach(f -> result.putAll(loadPropertiesFile(f, loadedFiles)));
    return result;
  }

}
