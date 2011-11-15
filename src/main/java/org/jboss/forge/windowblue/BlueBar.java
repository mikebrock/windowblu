package org.jboss.forge.windowblue;

import org.fusesource.jansi.Ansi;
import org.jboss.forge.shell.BufferManager;
import org.jboss.forge.shell.Shell;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Brock
 */
public class BlueBar {
  private static final String SAVE_POS = new String(new char[]{27, '7'});
  private static final String RES_POS = new String(new char[]{27, '8'});
  private static final String HOME = new String(new char[]{27, '[', 'H'});
  private static final String ERASE_TO_END = new String(new char[]{27, '[', 'K'});


  private byte[] render;
  private int width;
  private final BlueBufferManager manager;
  private Shell shell;

  private Ansi.Color titleBarColor;
  private Ansi.Color textColor;

  public BlueBar(BlueBufferManager manager, Shell shell) {
    this.manager = manager;
    this.shell = shell;
    this.width = manager.getWidth();
    this.render = new byte[width];
    loadDefaults();
  }

  private void loadDefaults() {
    titleBarColor = Ansi.Color.BLUE;
    textColor = Ansi.Color.BLACK;
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

  private static final String FORGE_NAME = "Forge " + Shell.class.getPackage().getImplementationVersion();

  public void render() {
    synchronized (manager) {
        manager.directWrite(SAVE_POS);
        manager.directWrite(HOME);
        manager.directWrite(attr(30, 44));

        StringBuilder sb = new StringBuilder()
                .append(new Date().toString())
                .append(" | ")
                .append(shell.getCurrentDirectory().getFullyQualifiedName())
                .append(" | ");

        manager.directWrite(sb.toString());

        int toPad = width - sb.length() - FORGE_NAME.length();
        manager.directWrite(pad(toPad));

        manager.directWrite(attr(1, 37));
        manager.directWrite(FORGE_NAME);

        manager.directWrite(RES_POS);

    }
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
          buf.append(" ").append(el);
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

  public static void main(String[] args) {
    List<String> parts = new ArrayList<String>();
    parts.add(new Date().toString());
    parts.add("JBoss Forge");
    parts.add("akdsjflkjdaslk/dsajlkfjlaksdjfdsa/");

    System.out.println(renderCols(parts, new boolean[]{false, false, true}));

  }

  private String attr(int... code) {
    return new String(new char[]{27, '['}) + _attr(code) + "m";
  }

  private String _attr(int... code) {
    StringBuilder b = new StringBuilder();
    boolean first = true;
    for (int c : code) {
      if (!first) {
        b.append(';');
      }
      first = false;
      b.append(c);
    }
    return b.toString();
  }
}
