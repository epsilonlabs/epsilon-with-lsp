/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.epsilon.lsp;

import java.net.URI;

public interface NativeTypeClassLoaderProvider {

	ClassLoader getNativeTypeClassLoader(URI sourceUri, ClassLoader fallbackClassLoader);

}
