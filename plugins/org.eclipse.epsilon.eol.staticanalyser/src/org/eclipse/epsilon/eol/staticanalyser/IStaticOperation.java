package org.eclipse.epsilon.eol.staticanalyser;

import java.util.List;

import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public interface IStaticOperation {

	public String getName();
	
	public EolType getContextType();
	
	public EolType getReturnType();
	
	public List<Parameter> getParameters();
}
