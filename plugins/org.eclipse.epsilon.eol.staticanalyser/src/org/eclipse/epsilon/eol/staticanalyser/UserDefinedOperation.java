package org.eclipse.epsilon.eol.staticanalyser;

import java.util.List;

import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class UserDefinedOperation implements IStaticOperation {
	private String name;
	private EolType contextType;
	private EolType returnType;
	private List<Parameter> parameters;
	
	public UserDefinedOperation(Operation op) {
		name = op.getName();
		contextType = (EolType) op.getData().get("contextType");
		returnType = (EolType) op.getData().get("returnType");
		parameters = op.getFormalParameters();
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public EolType getContextType() {
		return contextType;
	}

	@Override
	public EolType getReturnType() {
		return returnType;
	}

	@Override
	public List<Parameter> getParameters() {
		return parameters;
	}

}
