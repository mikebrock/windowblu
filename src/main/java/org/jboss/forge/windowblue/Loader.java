package org.jboss.forge.windowblue;

import org.jboss.forge.shell.BufferManager;
import org.jboss.forge.shell.Shell;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import java.util.Set;

/**
 * @author Mike Brock
 */
public class Loader implements Extension {
  private BlueBufferManager blueBufferManager;
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

  public void afterStartup(@Observes AfterBeanDiscovery beanDiscovery, BeanManager beanManager) {
    Set<Bean<?>> beans = beanManager.getBeans(Shell.class);
    Bean<?> bean = beanManager.resolve(beans);
    CreationalContext<?> context = beanManager.createCreationalContext(bean);
    Shell shell = (Shell) beanManager.getReference(bean, Shell.class, context);
    shell.clear();

    BufferManager manager = shell.getBufferManager();
    blueBufferManager = new BlueBufferManager(manager, shell);
    shell.registerBufferManager(blueBufferManager);
    updateThread.setPriority(Thread.MIN_PRIORITY);
    updateThread.start();
  }
}
