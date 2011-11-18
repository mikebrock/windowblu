package org.jboss.forge.windowblue;

import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.integration.BufferManager;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * @author Mike Brock
 */
public class BlueBufferManager implements BufferManager {
  private BlueBar blueBar;
  private BufferManager wrappedBuffer;

  private boolean bufferOnly = false;
  private ByteBuffer buffer;
  private int bufferSize = 0;
  private int maxBufferSize = 1024 * 10;

  private File backBufferDir = new File(System.getProperty("user.home") + "/.forge/windowblue/");
  private File backBuffer = new File(backBufferDir.getAbsolutePath() + "/buffer.log");

  private OutputStream bufferOut;
  private RandomAccessFile bufferIn;

  private boolean followBuffer = true;

  private static final String CLEAR_SCREEN = new String(new char[]{27, '[', '2', 'J'});
  private static final String HOME_POSITION = new String(new char[]{27, '[', '2', ';', '0', 'H'});

  public BlueBufferManager(BufferManager wrappedBuffer, Shell shell) {
    this.wrappedBuffer = wrappedBuffer;
    this.buffer = ByteBuffer.allocateDirect(maxBufferSize);
    this.blueBar = new BlueBar(this, shell);

    initBackBufferLog();
  }

  @Override
  public void bufferOnlyMode() {
    bufferOnly = true;
  }

  @Override
  public void directWriteMode() {
    bufferOnly = false;
    flushBuffer();
  }

  @Override
  public synchronized void flushBuffer() {
    String render = blueBar.render();
    wrappedBuffer.write(render);
    wrappedBuffer.bufferOnlyMode();
    byte[] buf = new byte[2048];
    buffer.rewind();
    do {
      int i = 0;
      Inner:
      for (; i < buf.length && bufferSize > 0; i++) {
        bufferSize--;
        switch (buf[i] = buffer.get()) {
          // intercept escape code
          case 27:
            if (i + 20 >= buf.length) {
              buffer.position(buffer.position() - 1);
              bufferSize++;
              break Inner;
            }
            if (bufferSize > 0) {
              bufferSize--;
              switch (buf[++i] = buffer.get()) {
                case '[':
                  if (bufferSize > 0) {
                    bufferSize--;
                    switch (buf[++i] = buffer.get()) {
                      case '2':
                        if (bufferSize > 0) {
                          bufferSize--;
                          switch (buf[++i] = buffer.get()) {
                            case 'J':
                              // clear screen intercepted
                              buf[++i] = 27;
                              buf[++i] = '[';
                              buf[++i] = '2';
                              buf[++i] = ';';
                              buf[++i] = '0';
                              buf[++i] = 'H';
                              // offset the buffersize.
                              break;
                          }
                        }
                    }
                  }
              }
            }

        }
      }
      wrappedBuffer.write(buf, 0, i);
    }
    while (bufferSize > 0);

    bufferSize = 0;
    buffer.clear();

    wrappedBuffer.write(render);
    wrappedBuffer.directWriteMode();
  }

  private boolean _write(int b) {
    try {
      bufferOut.write(b);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return followBuffer;
  }

  private boolean _write(byte[] b) {
    try {
      bufferOut.write(b);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return followBuffer;
  }

  private boolean _write(byte[] b, int offset, int length) {
    try {
      bufferOut.write(b, offset, length);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return followBuffer;
  }

  private boolean _write(String out) {
    try {
      bufferOut.write(out.getBytes());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return followBuffer;
  }


  private void _directWrite(String out) {
    try {
      bufferOut.write(out.getBytes());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    wrappedBuffer.directWrite(out);
  }


  private void _flush() {
    if (!bufferOnly) flushBuffer();
  }

  @Override
  public void write(int b) {
    if (!_write(b)) return;

    if (bufferSize + 1 >= maxBufferSize) {
      flushBuffer();
    }

    buffer.put((byte) b);
    bufferSize++;

    _write(b);
    _flush();
  }

  @Override
  public synchronized void write(byte b) {
    if (!_write(b)) return;

    if (bufferSize + 1 >= maxBufferSize) {
      flushBuffer();
    }

    buffer.put(b);
    bufferSize++;

    _flush();
  }

  @Override
  public synchronized void write(byte[] b) {
    if (!_write(b)) return;

    if (bufferSize + b.length >= maxBufferSize) {
      flushBuffer();
      write(b);
    }

    buffer.put(b);
    bufferSize += b.length;


    _flush();
  }

  @Override
  public synchronized void write(byte[] b, int offset, int length) {
    if (!_write(b, offset, length)) return;

    if (bufferSize + length >= maxBufferSize) {
      flushBuffer();
      write(b, offset, length);
    }

    buffer.put(b, offset, length);
    bufferSize += length;

    _flush();
  }

  @Override
  public void write(String s) {
    write(s.getBytes());
  }

  private void segmentedWrite(byte[] b, int offset, int length) {
    if (b.length > maxBufferSize) {

      int segs = b.length / maxBufferSize;
      int tail = b.length % maxBufferSize;
      for (int i = 0; i < segs; i++) {
        write(b, (i + offset) * maxBufferSize, maxBufferSize);
      }
      write(b, (segs + 1) * maxBufferSize, tail);
    }
    else {
      write(b, offset, length);
    }
  }

  public synchronized void directWrite(String s) {

    _directWrite(s);
  }

  @Override
  public void setBufferPosition(int row, int col) {
    buffer.position(row * wrappedBuffer.getWidth() + col);
  }

  @Override
  public int getHeight() {
    return wrappedBuffer.getHeight() - 1;
  }

  @Override
  public int getWidth() {
    return wrappedBuffer.getWidth();
  }

  public synchronized void render() {
    _write(blueBar.render());
    flushBuffer();
  }

  public synchronized void pageUp() {
    try {
      followBuffer = false;
      bufferOut.flush();

      long topOfPage = countBytesToCR(true, getHeight());
      if (topOfPage == -1) topOfPage = 0;

      bufferIn.seek(topOfPage);
      long toByte = countBytesToCR(true, getHeight());

      if (toByte == -1) {
        bufferIn.seek(topOfPage = 0);
        toByte = countBytesToCR(false, getHeight());
      }

      redrawFromPosition(toByte, topOfPage);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void pageDown() {
    try {
      bufferOut.flush();

      long topOfPage = bufferIn.getFilePointer();
      long toByte = countBytesToCR(false, getHeight());

      if (toByte == -1) {
        resetToNormal();
        return;
      }

      redrawFromPosition(toByte, topOfPage);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void resetToNormal() {
    try {
      followBuffer = true;
      long toByte = bufferIn.length();
      long topOfPage = countBytesToCR(true, getHeight());
      if (topOfPage == -1) {
        topOfPage = 0;
      }
      redrawFromPosition(toByte, topOfPage);
      flushBuffer();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void redrawFromPosition(long bytePosition, long toBytePosition) {
    try {
      if (bytePosition == -1 || toBytePosition == -1) return;

      bufferIn.seek(bytePosition);

      wrappedBuffer.bufferOnlyMode();
      wrappedBuffer.write(CLEAR_SCREEN);
      wrappedBuffer.write(blueBar.render());
      wrappedBuffer.write(HOME_POSITION);

      byte[] buf = new byte[1024];
      int read;
      int totalBytesRead = 0;

      while ((read = bufferIn.read(buf)) != -1) {
        for (int i = 0; i < read; i++) {
          if (++totalBytesRead >= totalBytesRead) break;
          wrappedBuffer.write(buf[i]);
        }
      }

      wrappedBuffer.directWriteMode();
    }
    catch (IOException e) {
      throw new RuntimeException("seek error");
    }
  }


  private synchronized long countBytesToCR(boolean reverse, int crs) throws IOException {
    byte[] buf = new byte[1024];

    int lineCount = 0;
    int col = 0;
    final int lineWidth = getWidth();


    if (reverse) {
      long pos = bufferIn.getFilePointer() - buf.length;
      if (pos < 0) pos = 0;

      for (; pos > 0; pos -= buf.length) {
        int read = bufferIn.read(buf);

        for (int i = 0; i < read; i++) {
          switch (buf[i]) {
            case '\n':
              lineCount++;
              col = 0;
              break;
            default:
              if (++col == lineWidth) {
                lineCount++;
                col = 0;
              }
          }
        }

        if (lineCount > crs) {
          int diff = lineCount - crs;
          for (int c = 0; c < read; c++) {
            switch (buf[c]) {
              case '\n':
                if (--diff == 0) {
                  return pos + c;
                }
                break;
              default:
                if (++col == lineWidth) {
                  lineCount++;
                  col = 0;
                }
            }
          }
        }
      }
    }
    else {
      long pos = bufferIn.getFilePointer() + buf.length;
      if (pos > bufferIn.length()) return -1;

      for (; pos < bufferIn.length(); pos += buf.length) {
        int read = bufferIn.read(buf);

        for (int i = 0; i < read; i++) {
          switch (buf[i]) {
            case '\n':
              lineCount++;
              col = 0;
              break;
            default:
              if (++col == lineWidth) {
                lineCount++;
                col = 0;
              }
          }
        }

        if (lineCount > crs) {
          int diff = lineCount - crs;
          for (int c = 0; c < read; c++) {
            switch (buf[c]) {
              case '\n':
                if (--diff == 0) {
                  return pos + c;
                }
                break;
              default:
                if (++col == lineWidth) {
                  lineCount++;
                  col = 0;
                }
            }
          }
        }
      }
    }

    return -1;
  }

  private void initBackBufferLog() {
    if (!backBufferDir.exists()) {
      backBufferDir.mkdirs();
    }

    try {
      bufferOut = new BufferedOutputStream(new FileOutputStream(backBuffer, true));
      bufferIn = new RandomAccessFile(backBuffer, "r");
    }
    catch (Exception e) {
      throw new RuntimeException("could not initialize buffer", e);
    }
  }
}
