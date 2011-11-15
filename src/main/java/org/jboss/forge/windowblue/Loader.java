package org.jboss.forge.windowblue;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.util.Set;

/**
 * @author Mike Brock
 */
public class Loader implements Extension {
  public void observes(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
    beanManager.fireEvent(new WindowBluUpdate());
  }
}
