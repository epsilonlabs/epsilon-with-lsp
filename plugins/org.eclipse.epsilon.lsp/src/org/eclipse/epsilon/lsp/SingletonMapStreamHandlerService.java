package org.eclipse.epsilon.lsp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * <p>
 * Stream handler service which first resolves URLs in the {@link #PROTOCOL} by
 * looking up the path in an in-memory map, and then falls back to the file with
 * the same path if no entry exists.
 * </p>
 * 
 * <p>
 * To register entries, retrieve the singleton {@link Registry} via
 * {@link Registry#getInstance()} and then use its
 * {@link Registry#putCode(String, String)} method.
 * </p>
 */
public class SingletonMapStreamHandlerService extends AbstractURLStreamHandlerService {

	public static final String PROTOCOL = "mapentry";

	public static class Registry {
		private static Registry INSTANCE;
		private Registry() {}

		public synchronized static Registry getInstance() {
			if (INSTANCE == null) {
				INSTANCE = new Registry();
			}
			return INSTANCE;
		}

		private final Map<String, String> PATH_TO_CODE = new HashMap<>();

		public String getCode(String path) {
			return PATH_TO_CODE.get(path);
		}

		public void putCode(String path, String code) {
			PATH_TO_CODE.put(path, code);
		}

		public void removeCode(String path) {
			PATH_TO_CODE.remove(path);
		}

		public void clear() {
			PATH_TO_CODE.clear();
		}
	}
	
	public URLConnection openConnection(URL u) throws IOException {
		String code = Registry.getInstance().getCode(u.getPath());
		if (code != null) {
			return new MapEntryURLConnection(u, code);
		} else {
			try {
				// Fall back to a file URL if we have no entry for this
				return new URI("file", u.getHost(), u.getPath(), null).toURL().openConnection();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Dummy URL connection fed by an in-memory map entry.
	 */
	protected static class MapEntryURLConnection extends URLConnection {
		private final byte[] content;

		protected MapEntryURLConnection(URL url, String content) {
			super(url);
			this.content = content.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public void connect() throws IOException {
			// nothing to do!
		}

		@Override
		public String getContentType() {
			return "text/plain";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(content);
		}

		@Override
		public long getContentLengthLong() {
			return content.length;
		}

		@Override
		public String getContentEncoding() {
			return StandardCharsets.UTF_8.name();
		}
	}
}