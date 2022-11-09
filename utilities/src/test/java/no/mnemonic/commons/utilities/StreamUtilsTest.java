package no.mnemonic.commons.utilities;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StreamUtilsTest {

  @Test(expected = IllegalArgumentException.class)
  public void writeUntilEOFWithNullInputStream() throws IOException {
    StreamUtils.writeUntilEOF(null, new ByteArrayOutputStream());
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeUntilEOFWithNullOutputStream() throws IOException {
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(new byte[]{}), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeUntilEOFWithZeroBufsize() throws IOException {
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(), 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeUntilEOFWithNegativeBufsize() throws IOException {
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(new byte[]{}), new ByteArrayOutputStream(), -1);
  }

  @Test
  public void writeUntilEOF() throws IOException {
    byte[] data = new byte[]{1,2,3,4,5,6,7,8,9,10};
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(data), os);
    assertTrue(Arrays.equals(data, os.toByteArray()));
  }

  @Test
  public void writeUntilEOFWithOversizeBuflen() throws IOException {
    byte[] data = new byte[]{1,2,3,4,5,6,7,8,9,10};
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(data), os, 1000);
    assertTrue(Arrays.equals(data, os.toByteArray()));
  }

  @Test
  public void writeUntilEOFWithEmptyArray() throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(new byte[]{}), os);
    assertEquals(0, os.toByteArray().length);
  }

  @Test
  public void writeUntilEOFWithStreamProgressMonitor() throws IOException {
    byte[] data = new byte[]{1,2,3,4,5,6,7,8,9,10};
    StreamUtils.StreamProgressMonitor monitor = mock(StreamUtils.StreamProgressMonitor.class);
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(data), new ByteArrayOutputStream(), 100, monitor);
    verify(monitor).progressReport(10);
    verify(monitor, times(1)).progressReport(anyInt());
  }

  @Test
  public void writeUntilEOFWithStreamProgressMonitorMultipleReads() throws IOException {
    byte[] data = new byte[]{1,2,3,4,5,6,7,8,9,10};
    StreamUtils.StreamProgressMonitor monitor = mock(StreamUtils.StreamProgressMonitor.class);
    StreamUtils.writeUntilEOF(new ByteArrayInputStream(data), new ByteArrayOutputStream(), 5, monitor);
    verify(monitor).progressReport(5);
    verify(monitor).progressReport(10);
    verify(monitor, times(2)).progressReport(anyInt());
  }

  @Test
  public void readFullStreamTest() throws IOException {
    byte[] data = new byte[]{1,2,3,4,5,6,7,8,9,10};
    InputStream is= new ByteArrayInputStream(data);
    byte[] readData = StreamUtils.readFullStream(is, true);
    assertArrayEquals(data, readData);
  }
}
