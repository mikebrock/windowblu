package org.jboss.forge.windowblue;

import org.fusesource.jansi.Ansi;
import org.jboss.forge.shell.BufferManager;
import org.jboss.forge.shell.Shell;

import java.text.SimpleDateFormat;
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

  private int width;
  private final BlueBufferManager manager;
  private Shell shell;

  private Ansi.Color titleBarColor;
  private Ansi.Color textColor;

  public BlueBar(BlueBufferManager manager, Shell shell) {
    this.manager = manager;
    this.shell = shell;
    this.width = manager.getWidth();
    loadDefaults();
  }

  private void loadDefaults() {
    titleBarColor = Ansi.Color.BLUE;
    textColor = Ansi.Color.BLACK;
  }


  private static final String FORGE_NAME = "Forge " + Shell.class.getPackage().getImplementationVersion();
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d yyyy kk:mm");

  public String render() {
    synchronized (manager) {
      this.width = manager.getWidth();

      StringBuilder buf = new StringBuilder(SAVE_POS)
              .append(SAVE_POS)
              .append(HOME)
              .append(attr(30, 44));

      StringBuilder contents = new StringBuilder(DATE_FORMAT.format(new Date()))
                    .append(" | ")
                    .append(shell.getCurrentDirectory().getFullyQualifiedName())
                    .append(" | ");

      buf.append(contents);

      int toPad = width - contents.length() - FORGE_NAME.length();

      return buf.append(pad(toPad))
              .append(attr(1, 37))
              .append(FORGE_NAME)
              .append(RES_POS).toString();
    }
  }

  public static String pad(final int amount) {
    char[] padding = new char[amount];
    for (int i = 0; i < amount; i++) {
      padding[i] = ' ';
    }
    return new String(padding);
  }

  private static String attr(int... code) {
    return new String(new char[]{27, '['}) + _attr(code) + "m";
  }

  private static String _attr(int... code) {
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
