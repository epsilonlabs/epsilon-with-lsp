package org.eclipse.epsilon.lsp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores unsaved document contents for the {@value #PROTOCOL} URL protocol.
 */
public final class MapEntryRegistry {

	public static final String PROTOCOL = "mapentry";

	private static final MapEntryRegistry INSTANCE = new MapEntryRegistry();
	private final Map<String, String> pathToCode = new ConcurrentHashMap<>();

	private MapEntryRegistry() {
	}

	public static MapEntryRegistry getInstance() {
		return INSTANCE;
	}

	public String getCode(String path) {
		return pathToCode.get(canonicalPath(path));
	}

	public void putCode(String path, String code) {
		pathToCode.put(canonicalPath(path), code);
	}

	public void removeCode(String path) {
		pathToCode.remove(canonicalPath(path));
	}

	public void clear() {
		pathToCode.clear();
	}

	public URLConnection openConnection(URL url) throws IOException {
		try {
			String path = url.toURI().getPath();
			String code = getCode(path);
			if (code != null) {
				return new MapEntryURLConnection(url, code);
			}
			return new URI("file", url.getHost(), path, null).toURL().openConnection();
		}
		catch (URISyntaxException ex) {
			throw new IOException("Invalid map entry URL " + url, ex);
		}
	}

	/** Returns one stable identity for file and {@value #PROTOCOL} URI aliases. */
	static URI canonicalFileUri(URI uri) {
		URI normalized = uri.normalize();
		if (PROTOCOL.equals(normalized.getScheme())) {
			try {
				normalized = new URI("file", normalized.getAuthority(), normalized.getPath(), null, null);
			}
			catch (URISyntaxException ex) {
				return normalized;
			}
		}
		if (!"file".equalsIgnoreCase(normalized.getScheme())) {
			return normalized;
		}

		try {
			return new File(normalized).getCanonicalFile().toURI();
		}
		catch (IOException | IllegalArgumentException | SecurityException ex) {
			return normalized;
		}
	}

	private static String canonicalPath(String path) {
		try {
			return canonicalFileUri(new URI("file", null, path, null)).getPath();
		}
		catch (URISyntaxException | IllegalArgumentException ex) {
			return path;
		}
	}

	private static class MapEntryURLConnection extends URLConnection {
		private final byte[] content;

		MapEntryURLConnection(URL url, String content) {
			super(url);
			this.content = content.getBytes(StandardCharsets.UTF_8);
		}

		@Override
		public void connect() throws IOException {
			// Nothing to connect to: the content is already in memory.
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
