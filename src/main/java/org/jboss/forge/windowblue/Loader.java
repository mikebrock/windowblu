package org.jboss.forge.windowblue;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

/**
 * @author Mike Brock
 */
public class Loader implements Extension {
  public void observes(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
  //  beanManager.fireEvent(new WindowBluUpdate());
  }
}
