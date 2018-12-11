package no.mnemonic.commons.junit.docker;


import no.mnemonic.commons.utilities.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerTestUtils {

  private static final String ENVIRONMENT_VARIABLE = System.getenv("DOCKER_HOST");
  private static final String DOCKER_HOST = extractHost(ENVIRONMENT_VARIABLE);

  /**
   * @return the value of the DOCKER_HOST environment variable
   */
  public static String getDockerHostVariable() {
    return ENVIRONMENT_VARIABLE;
  }

  /**
   * @return the hostname extracted from the DOCKER_HOST variable.
   * Currently, this tool supports tcp://hostname:port urls.
   * If DOCKER_HOST is a unix:// socket, or if DOCKER_HOST variable is not set, this method will return "localhost".
   */
  public static String getDockerHost() {
    return DOCKER_HOST;
  }

  static String extractHost(String dockerHost) {
    if (StringUtils.isBlank(dockerHost)) {
      return "localhost";
    } else if (dockerHost.matches("unix://.*")){
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