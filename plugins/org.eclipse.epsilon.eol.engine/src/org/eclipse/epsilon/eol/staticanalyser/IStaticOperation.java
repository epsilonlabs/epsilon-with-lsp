package org.eclipse.epsilon.eol.staticanalyser;

import java.util.List;

import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public interface IStaticOperation {

	public String getName();
	
	public EolType getContextType();
	
	public EolType getReturnType(EolType actualContextType, List<EolType> actualParameterTypes);
	
	public List<EolType> getParameterTypes();
}
