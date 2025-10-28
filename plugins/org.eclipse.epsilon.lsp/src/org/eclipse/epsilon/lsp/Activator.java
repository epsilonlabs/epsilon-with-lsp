package org.eclipse.epsilon.lsp;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLStreamHandlerService;

public class Activator implements BundleActivator {

    private ServiceRegistration<?> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("url.handler.protocol", "mapentry");
        registration = context.registerService(URLStreamHandlerService.class.getName(),
                new SingletonMapStreamHandlerService(), props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
    
}
