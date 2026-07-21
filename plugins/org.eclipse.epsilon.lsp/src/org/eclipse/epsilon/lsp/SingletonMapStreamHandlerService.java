package org.eclipse.epsilon.lsp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * <p>
 * Stream handler service which first resolves URLs in the {@link #PROTOCOL} by
 * looking up the path in an in-memory map, and then falls back to the file with
 * the same path if no entry exists.
 * </p>
 * 
 * Entries are managed through {@link MapEntryRegistry}.
 */
public class SingletonMapStreamHandlerService extends AbstractURLStreamHandlerService {

	public static final String PROTOCOL = MapEntryRegistry.PROTOCOL;
	
	@Override
	public URLConnection openConnection(URL u) throws IOException {
		return MapEntryRegistry.getInstance().openConnection(u);
	}
}
