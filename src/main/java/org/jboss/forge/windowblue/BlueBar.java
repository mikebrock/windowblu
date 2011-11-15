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
  private static final char FIRST_ESC_CHAR = 27;
  private static final char SECOND_ESC_CHAR = '['

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

  public void render() {
    synchronized (manager) {
      manager.directWrite(getEscape('7'));

      Ansi a = new Ansi().bg(titleBarColor).fg(textColor);

      List<String> parts = new ArrayList<String>();
      parts.add(new Date().toString());
      parts.add("JBoss Forge");
      parts.add(shell.getCurrentDirectory().getFullyQualifiedName());

      a.a(renderCols(parts, new boolean[]{false, false, true}));

      manager.directWrite(a.toString());

      manager.directWrite(getEscape('8'));
    }

    //  manager.flushBuffer();
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

  private String getEscape(char command) {
 		return new StringBuilder().append(FIRST_ESC_CHAR).append(SECOND_ESC_CHAR).append(command).toString();
 	}
}
