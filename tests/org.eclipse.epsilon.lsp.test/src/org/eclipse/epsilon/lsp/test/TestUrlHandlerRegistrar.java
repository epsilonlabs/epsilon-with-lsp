package org.eclipse.epsilon.lsp.test;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.epsilon.lsp.SingletonMapStreamHandlerService;

public final class TestUrlHandlerRegistrar {

    private static final AtomicBoolean registered = new AtomicBoolean(false);

    public static void registerSingletonMapHandlerOnce() {
        if (!registered.compareAndSet(false, true)) return;

        try {
            URL.setURLStreamHandlerFactory(new TestFactory());
        } catch (Error e) {
            // factory already set in this JVM (e.g. by other tests); ignore
        }
    }

    private static class TestFactory implements URLStreamHandlerFactory {
        private final SingletonMapStreamHandlerService service = new SingletonMapStreamHandlerService();

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            if (SingletonMapStreamHandlerService.PROTOCOL.equals(protocol)) {
                return new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        try {
                            return service.openConnection(u);
                        } catch (IOException e) {
                            throw e;
                        }
                    }
                };
            }
            return null;
        }
    }

//    private TestUrlHandlerRegistrar() {}
}
