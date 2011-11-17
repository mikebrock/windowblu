package org.jboss.forge.windowblue;

import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.events.Shutdown;
import org.jboss.forge.shell.integration.BufferManager;
import org.jboss.forge.shell.integration.KeyListener;
import org.jboss.forge.shell.plugins.*;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Calendar;

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
    shell.registerKeyListener(new KeyListener() {
      @Override
      public boolean keyPress(int key) {
        switch (key) {
          case 1: // CTRL-A;
            System.out.println("Back Buffer");
            break;
        }
        return false;
      }
    });
    
    running = true;
    updateThread.setPriority(Thread.MIN_PRIORITY);
    updateThread.start();
  }

  private Thread updateThread = new Thread() {
    @Override
    public void run() {
      for (; ; ) {
        try {
          if (!running) return;
           blueBufferManager.render();
          Thread.sleep(calculateSleep());
        }
        catch (InterruptedException e) {
          // ignore;
        }
        catch (IllegalStateException e) {
          return;
        }
      }
    }
  };

  private boolean initial = true;
  private long calculateSleep() {
    if (initial) {
      initial = false;
      Calendar c = Calendar.getInstance();
      c.add(Calendar.MINUTE, 1);      
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      
      return c.getTimeInMillis() - System.currentTimeMillis();
    }
    else {
      return 60000;
    }
  }
  

  public void update(@Observes WindowBluUpdate update) {
    blueBufferManager.render();
  }

  public void startup(@Observes Shutdown shutdown) {
    running = false;
    updateThread.interrupt();
  }

  @DefaultCommand
  public void windowblu(PipeOut out) {
    out.println("WindowBlu by Mike Brock");
  }


}
