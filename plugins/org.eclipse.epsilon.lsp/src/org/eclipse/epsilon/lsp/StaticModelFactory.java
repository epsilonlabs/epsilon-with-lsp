package org.eclipse.epsilon.lsp;

import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.staticanalyser.IModelFactory;

public class StaticModelFactory implements IModelFactory {

	@Override
	public IModel createModel(String driver) {
		switch (driver){
			case "EMF": return new EmfModel();
			default: return null;
				
		}
	}

}