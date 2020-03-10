package no.mnemonic.commons.testtools;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.NoSuchElementException;
import java.util.Random;

public class AvailablePortFinder {

  private static final int MIN_PORT = 1;
  private static final int MAX_PORT = 65535;
  private static final int DEFAULT_OFFSET_RANGE = 2048;
  private static final Random RANDOM = new Random();

  private AvailablePortFinder() {
  }

  /**
   * Finds an open port. Both TCP and UDP port must be open.
   * <p>
   * Tests ports starting with 'start' port plus a random offset (0-2048)
   *
   * @param start First port to test.
   * @return Returns an open port.
   * @throws NoSuchElementException If no open port could be found.
   */
  public static int getAvailablePort(int start) {
    return getAvailablePort(start, DEFAULT_OFFSET_RANGE);
  }

  /**
   * Finds an open port. Both TCP and UDP port must be open.
   * <p>
   * Tests ports starting with 'start' port plus a random offset (0-offsetRange)
   *
   * @param start First port to test.
   * @param offsetRange The size of the port range to select within.
   * @return Returns an open port.
   * @throws NoSuchElementException If no open port could be found.
   */
  public static int getAvailablePort(int start, int offsetRange) {
    if (start < MIN_PORT || start > MAX_PORT) {
      throw new IllegalArgumentException("Illegal start port: " + start);
    }
    if (offsetRange < 1) {
      throw new IllegalArgumentException("Illegal offset range: " + offsetRange);
    }
    if (start + offsetRange > MAX_PORT) {
      throw new IllegalArgumentException("Offset range above max port: " + (start + offsetRange));
    }

    int port = start + RANDOM.nextInt(offsetRange);
    while (port <= MAX_PORT) {
      if (isPortAvailable(port)) return port;
      port++;
    }

    throw new NoSuchElementException("No port available above port " + start);
  }

  private static boolean isPortAvailable(int port) {
    // Test both for TCP and UDP.
    try (ServerSocket tcp = new ServerSocket(port);
         DatagramSocket udp = new DatagramSocket(port)) {
      tcp.setReuseAddress(true);
      udp.setReuseAddress(true);
      return true;
    } catch (IOException e) {
      // Ignore exception. Port is unavailable.
    }

    return false;
  }

}
