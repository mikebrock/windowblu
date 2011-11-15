package org.jboss.forge.windowblue;

import com.sun.org.apache.bcel.internal.classfile.InnerClass;
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
    wrappedBuffer.bufferOnlyMode();
    byte[] buf = new byte[2048];
    buffer.rewind();
    do {
      int i = 0;
      Inner:
      for (; i < buf.length && bufferSize > 0; i++) {
        switch (buf[i] = buffer.get()) {
          // intercept escape code
          case 27:
            if (i + 10 >= buf.length) {
              buffer.position(buffer.position() - 1);
              i--;
              break Inner;
            }
            switch (buf[++i] = buffer.get()) {
              case '[':
                switch (buf[++i] = buffer.get()) {
                  case '2':
                    switch (buf[++i] = buffer.get()) {
                      case 'J':
                        // clear screen intercepted
                        buf[++i] = 27;
                        buf[++i] = '[';
                        buf[++i] = 'H';
                        buf[++i] = 27;
                        buf[++i] = '[';
                        buf[++i] = '1';
                        buf[++i] = 'B';
                        break;
                    }
                }
            }

        }
        bufferSize--;
      }
      wrappedBuffer.write(buf, 0, i);
    }
    while (bufferSize > 0);

    bufferSize = 0;
    buffer.clear();
    wrappedBuffer.flushBuffer();
    wrappedBuffer.directWriteMode();
    blueBar.render();
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
