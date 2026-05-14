package org.eclipse.epsilon.lsp;

import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.plainxml.LspDriverPlainXml;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.UnknownModel;
import org.eclipse.epsilon.eol.staticanalyser.IModelFactory;

public class StaticModelFactory implements IModelFactory {

	@Override
	public IModel createModel(String driver) {
		switch (driver){
			case "EMF": return new EmfModel();
			case "Unknown": return new UnknownModel();
			case "PlainXml" : return new LspDriverPlainXml();
			default: return null;
				
		}
	}

}