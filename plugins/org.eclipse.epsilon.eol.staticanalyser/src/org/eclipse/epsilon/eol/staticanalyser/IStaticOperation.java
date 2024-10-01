package org.eclipse.epsilon.eol.staticanalyser;

import java.util.List;

import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public interface IStaticOperation {

	public String getName();
	
	public EolType getContextType();
	
	//TODO: We might need the parameter types for this
	public EolType getReturnType();
	
	public List<EolType> getParameterTypes();
}
