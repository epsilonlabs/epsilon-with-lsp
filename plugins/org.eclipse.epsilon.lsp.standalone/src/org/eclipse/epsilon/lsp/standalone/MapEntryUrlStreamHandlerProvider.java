package org.eclipse.epsilon.lsp.standalone;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

import org.eclipse.epsilon.lsp.MapEntryRegistry;

public class MapEntryUrlStreamHandlerProvider extends URLStreamHandlerProvider {

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (!MapEntryRegistry.PROTOCOL.equals(protocol)) {
			return null;
		}

		return new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL url) throws IOException {
				return MapEntryRegistry.getInstance().openConnection(url);
			}
		};
	}
}
