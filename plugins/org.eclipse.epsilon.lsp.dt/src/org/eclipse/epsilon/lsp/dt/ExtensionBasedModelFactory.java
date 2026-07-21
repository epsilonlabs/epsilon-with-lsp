package org.eclipse.epsilon.lsp.dt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.common.dt.launching.extensions.ModelTypeExtension;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.models.IModel;

public class ExtensionBasedModelFactory implements IModelFactory {

	@Override
	public IModel createModel(String driver) {
		try {
			return ModelTypeExtension.forType(driver).createModel();
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

}
