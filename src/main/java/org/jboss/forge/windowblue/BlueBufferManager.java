package org.jboss.forge.windowblue;

import org.jboss.forge.shell.BufferManager;
import org.jboss.forge.shell.Shell;

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

  public BlueBufferManager(BufferManager wrappedBuffer, Shell shell) {
    this.wrappedBuffer = wrappedBuffer;
    this.buffer = ByteBuffer.allocateDirect(wrappedBuffer.getHeight() * wrappedBuffer.getWidth() * 4);
    this.blueBar = new BlueBar(this, shell);
  }

  @Override
  public void bufferOnlyMode() {
    bufferOnly = true;
  }

  @Override
  public void directWriteMode() {
    bufferOnly = false;
  }

  @Override
  public synchronized void flushBuffer() {
    byte[] buf = new byte[1024];
    buffer.rewind();
    do {
      int i = 0;
      for (; i < buf.length && bufferSize > 0; i++) {
        buf[i] = buffer.get();
        bufferSize--;
      }
      wrappedBuffer.write(buf, 0, i);
      blueBar.render();
    }
    while (bufferSize > 0);

    bufferSize = 0;
    buffer.clear();
  }

  private void _flush() {
    if (!bufferOnly) flushBuffer();
  }

  @Override
  public synchronized void write(byte b) {
    buffer.put(b);
    bufferSize++;
    _flush();
  }

  @Override
  public synchronized void write(byte[] b) {
    buffer.put(b);
    bufferSize += b.length;
    _flush();
  }

  @Override
  public synchronized void write(byte[] b, int offset, int length) {
    buffer.put(b, offset, length);
    bufferSize += length;
    _flush();
  }

  @Override
  public void write(String s) {
    write(s.getBytes());
  }

  public synchronized void directWrite(String s) {
    wrappedBuffer.write(s);
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
    blueBar.render();
  }
}
