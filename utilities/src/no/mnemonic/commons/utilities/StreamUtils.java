package no.mnemonic.commons.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtils {

  private StreamUtils() {
  }

  /**
   * Common interface to be notified on stream read operations
   */
  public interface StreamProgressMonitor {
    void progressReport(int bytesRead);
  }

  /**
   * Same as {@link #writeUntilEOF(InputStream, OutputStream, int, StreamProgressMonitor...)},
   * with default read buffer size and no monitors.
   */
  public static void writeUntilEOF(InputStream is, OutputStream os) throws IOException {
    writeUntilEOF(is, os, 1024);
  }

  /**
   * Simply spools al data from inputstream to outputstream until the inputstream signals EOF. Beware, if inputstream is
   * seriously delayed, or a non-terminating stream, this method may block for a long time.
   * Stream is not closed upon EOF.
   *
   * @param is input stream to read from
   * @param os outputstream to write to
   * @param bufsize size of read buffer
   * @param monitors monitors to notify. Will be notified each time the read buffer is written
   * @throws IOException if an exception occurs while reading
   */
  public static void writeUntilEOF(InputStream is, OutputStream os, int bufsize, StreamProgressMonitor... monitors) throws IOException {
    if (is == null) throw new IllegalArgumentException("Inputstream is null");
    if (os == null) throw new IllegalArgumentException("Outputstream is null");
    if (bufsize <= 0) throw new IllegalArgumentException("Invalid bufsize: " + bufsize);
    byte[] buffer = new byte[bufsize];
    int read;
    int totalread = 0;
    //noinspection NestedAssignment
    while ((read = is.read(buffer)) >= 0) {
      os.write(buffer, 0, read);
      totalread += read;
      for (StreamProgressMonitor monitor : monitors) monitor.progressReport(totalread);
    }
  }
}
