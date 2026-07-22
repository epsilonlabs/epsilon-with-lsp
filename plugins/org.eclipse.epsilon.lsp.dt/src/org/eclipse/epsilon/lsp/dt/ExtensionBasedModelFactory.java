package org.eclipse.epsilon.lsp.dt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.epsilon.common.dt.launching.extensions.ModelTypeExtension;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.UnknownModel;

public class ExtensionBasedModelFactory implements IModelFactory {

	@Override
	public IModel createModel(String driver) {
		if ("Unknown".equals(driver)) {
			return new UnknownModel();
		}
		ModelTypeExtension modelType = ModelTypeExtension.forType(driver);
		if (modelType == null) {
			return null;
		}
		try {
			return modelType.createModel();
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

}
