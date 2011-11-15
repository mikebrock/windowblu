package org.jboss.forge.windowblue;

import com.sun.org.apache.xerces.internal.impl.dv.xs.AnySimpleDV;
import org.fusesource.jansi.Ansi;
import org.jboss.forge.shell.BufferManager;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellPrintWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Brock
 */
public class BlueBar {
  private byte[] render;
  private int width;
  private BufferManager manager;
  private Shell shell;

  private Ansi.Color titleBarColor;
  private Ansi.Color textColor;

  public BlueBar(BufferManager manager, Shell shell) {
    this.manager = manager;
    this.shell = shell;
    this.width = manager.getWidth();
    this.render = new byte[width];
  }

  private void loadDefaults() {
    titleBarColor = Ansi.Color.BLUE;
    textColor = Ansi.Color.WHITE;
  }

  private void _checkWidth() {
    if (width != manager.getWidth()) {
      width = manager.getWidth();
      resize();
    }
  }

  private void resize() {
    render();
  }

  public void render() {
    Ansi a = new Ansi().saveCursorPosition().cursor(0, 0).bg(titleBarColor).fg(textColor);

    List<String> parts = new ArrayList<String>();
    parts.add(new Date().toString());
    parts.add("JBoss Forge");
    parts.add(shell.getCurrentDirectory().getFullyQualifiedName());

    a.a(renderCols(parts, new boolean[] { false, false }));

    manager.write(a.restorCursorPosition().toString());
    manager.flushBuffer();
  }

  public static String renderCols(final List<String> list, final boolean[] columns) {
    int cols = columns.length;
    int[] colSizes = new int[columns.length];

    Iterator<String> iter = list.iterator();
    StringBuilder buf = new StringBuilder();

    String el;
    while (iter.hasNext()) {
      for (int i = 0; i < cols; i++) {
        if (colSizes[i] < (el = iter.next()).length()) {
          colSizes[i] = el.length();
        }
      }
    }

    iter = list.iterator();

    while (iter.hasNext()) {
      for (int i = 0; i < cols; i++) {
        el = iter.next();
        if (columns[i]) {
          buf.append(pad(colSizes[i] - el.length())).append(el);

          if (iter.hasNext()) {
            buf.append(" ");

          }
        }
        else {
          buf.append(" ");
          if (iter.hasNext()) {
            buf.append(pad(colSizes[i] - el.length())).append(" ");
          }
        }
      }
    }
    
    return buf.toString();
  }

  public static String pad(final int amount) {
    char[] padding = new char[amount];
    for (int i = 0; i < amount; i++) {
      padding[i] = ' ';
    }
    return new String(padding);
  }
}
