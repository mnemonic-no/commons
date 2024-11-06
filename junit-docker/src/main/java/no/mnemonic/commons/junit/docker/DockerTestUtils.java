package no.mnemonic.commons.junit.docker;

import no.mnemonic.commons.utilities.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for working with Docker environments during unit tests.
 *
 * @deprecated Use jupiter-docker instead
 */
@Deprecated
public class DockerTestUtils {

  private static final String ENVIRONMENT_VARIABLE = System.getenv("DOCKER_HOST");
  private static final String DOCKER_HOST = extractHost(ENVIRONMENT_VARIABLE);

  private DockerTestUtils() {
  }

  /**
   * Return the value of the DOCKER_HOST environment variable.
   *
   * @return DOCKER_HOST environment variable
   */
  public static String getDockerHostVariable() {
    return ENVIRONMENT_VARIABLE;
  }

  /**
   * Return the hostname extracted from the DOCKER_HOST environment variable.
   * <p>
   * This method supports URLs of the format "tcp://hostname:port". If DOCKER_HOST is a unix:// socket
   * or if DOCKER_HOST is not set, this method will return "localhost".
   *
   * @return Extracted hostname
   */
  public static String getDockerHost() {
    return DOCKER_HOST;
  }

  static String extractHost(String dockerHost) {
    if (StringUtils.isBlank(dockerHost)) {
      return "localhost";
    } else if (dockerHost.matches("unix://.*")) {
      return "localhost";
    } else {
      Pattern p = Pattern.compile("tcp://(.+):(.+)");
      Matcher m = p.matcher(dockerHost);
      if (!m.matches()) {
        throw new IllegalArgumentException("Illegal docker host: " + dockerHost);
      }
      return m.group(1);
    }
  }
}