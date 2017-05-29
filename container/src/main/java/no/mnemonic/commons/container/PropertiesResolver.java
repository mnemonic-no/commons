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

public class PropertiesResolver {

  static final String INCLUDE_FILE_PREFIX = "include.file.";
  private static final Logger LOGGER = Logging.getLogger(PropertiesResolver.class);

  public static Properties loadPropertiesFile(File file) {
    return loadPropertiesFile(file, new Properties());
  }

  public static Properties loadPropertiesFile(File file, Properties properties) {
    Set<File> loadedFiles = new HashSet<>();
    loadPropertiesFile(properties, file, loadedFiles);
    return properties;
  }

  private static void loadPropertiesFile(Properties properties, File file, Set<File> loadedFiles) {
    if (loadedFiles.contains(file)) {
      LOGGER.info("Skipping previously loaded file: " + file);
      return;
    }
    LOGGER.info("Loading properties from file: " + file);
    Properties newProps = new Properties();
    try (InputStream is = new FileInputStream(file)) {
      newProps.load(is);
    } catch (IOException e) {
      throw new RuntimeException("Could not load property file: " + file);
    }
    loadedFiles.add(file);
    resolveIncludes(newProps, loadedFiles);
    properties.putAll(newProps);
  }

  private static void resolveIncludes(Properties properties, Set<File> loadedFiles) {
    MapUtils.map(properties)
            .entrySet()
            .stream()
            .filter(e -> String.valueOf(e.getKey()).startsWith(INCLUDE_FILE_PREFIX))
            .map(e -> new File(String.valueOf(e.getValue())))
            .forEach(f -> loadPropertiesFile(properties, f, loadedFiles));
  }

}
