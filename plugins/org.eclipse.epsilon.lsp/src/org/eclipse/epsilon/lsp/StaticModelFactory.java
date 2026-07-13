package org.eclipse.epsilon.lsp;

import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.UnknownModel;

public class StaticModelFactory implements IModelFactory {

	@Override
	public IModel createModel(String driver) {
		switch (driver){
			case "EMF": return new EmfModel();
			case "Unknown": return new UnknownModel();
			default: return null;
				
		}
	}

}