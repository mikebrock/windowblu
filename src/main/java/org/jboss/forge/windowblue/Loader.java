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

    Set<Bean<?>> beans = beanManager.getBeans(WindowBlu.class);
    Bean<?> bean = beanManager.resolve(beans);
    CreationalContext<?> context = beanManager.createCreationalContext(bean);

    WindowBlu blue = (WindowBlu) beanManager.getReference(bean, WindowBlu.class, context);
 	}
}
