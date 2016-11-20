package no.mnemonic.commons.component;

/**
 * Interface for all classes implementing getPackageVersion()
 */
public interface Versioned {

  /**
   * @return Package version string, displaying the version for the package this class is a part of
   */
  String getPackageVersion();

}
