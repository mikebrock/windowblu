package org.jboss.forge.windowblue;

import org.jboss.forge.shell.BufferManager;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.events.*;
import org.jboss.forge.shell.plugins.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

/**
 * @author Mike Brock
 */
@ApplicationScoped
@Topic("Shell Environment")
@Alias("windowblu")
@Help("A Forge Plugin.")
public class WindowBlu implements Plugin {
  private Shell shell;
  private BlueBufferManager blueBufferManager;
  private boolean running = false;


  public WindowBlu() {
  }

  @Inject
  public WindowBlu(final Shell shell) {
    this.shell = shell;
    init();
  }

  public void init() {
    shell.clear();

    BufferManager manager = shell.getBufferManager();
    blueBufferManager = new BlueBufferManager(manager, shell);
    shell.registerBufferManager(blueBufferManager);
    running = true;
    updateThread.setPriority(Thread.MIN_PRIORITY);
    updateThread.start();
  }

  private Thread updateThread = new Thread() {
    @Override
    public void run() {
      try {
        for (; ; ) {
          blueBufferManager.render();
          Thread.sleep(1000);
        }
      }
      catch (InterruptedException e) {
        // ignore;
      }
    }
  };

  public void update(@Observes WindowBluUpdate update) {
    blueBufferManager.render();
  }
  
  public void startup(@Observes Shutdown shutdown) {
    running = false;
    updateThread.notify();
  }

  @DefaultCommand
  public void windowblu(PipeOut out) {
    out.println("WindowBlu by Mike Brock");
  }


}
