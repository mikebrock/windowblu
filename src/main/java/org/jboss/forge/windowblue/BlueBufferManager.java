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
    _write(render);
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
      _write(buf, 0, i);
    }
    while (bufferSize > 0);

    bufferSize = 0;
    buffer.clear();

    _write(render);
    wrappedBuffer.directWriteMode();
  }

  private void _write(byte[] b, int offset, int length) {
    try {
      bufferOut.write(b, offset, length);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    wrappedBuffer.write(b, offset, length);
  }

  private void _write(String out) {
    try {
      bufferOut.write(out.getBytes());
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    wrappedBuffer.write(out);
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
    if (bufferSize + 1 >= maxBufferSize) {
      flushBuffer();
    }

    buffer.put((byte) b);
    bufferSize++;
    _flush();
  }

  @Override
  public synchronized void write(byte b) {
    if (bufferSize + 1 >= maxBufferSize) {
      flushBuffer();
    }

    buffer.put(b);
    bufferSize++;
    _flush();
  }

  @Override
  public synchronized void write(byte[] b) {
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

  public void render() {
    _write(blueBar.render());
    flushBuffer();
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
