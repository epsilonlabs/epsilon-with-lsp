package org.eclipse.epsilon.eol.analyse;

import org.eclipse.epsilon.eol.models.IModel;

public interface IModelFactory {
	
	public IModel createModel(String driver);
	
}