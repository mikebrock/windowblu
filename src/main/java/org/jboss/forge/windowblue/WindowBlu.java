package org.jboss.forge.windowblue;

import org.jboss.forge.shell.BufferManager;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.Topic;

import javax.inject.Inject;

/**
 * @author Mike Brock
 */
@Topic("Shell Environment")
@Alias("windowblu")
@Help("A Forge Plugin.")
public class WindowBlu implements Plugin {
  private final Shell shell;
  private BlueBufferManager blueBufferManager;

  @Inject
  public WindowBlu(Shell shell) {
    this.shell = shell;
    init();
  }

  public void init() {
    shell.clear();

    BufferManager manager = shell.getBufferManager();
    blueBufferManager = new BlueBufferManager(manager, shell);
    shell.registerBufferManager(blueBufferManager);
    updateThread.setPriority(Thread.MIN_PRIORITY);
    updateThread.start();

    System.out.println("WindowBlu loaded");
  }

  private Thread updateThread = new Thread() {
    @Override
    public void run() {
      try {
        blueBufferManager.render();
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        // ignore;
      }
    }
  };
}
