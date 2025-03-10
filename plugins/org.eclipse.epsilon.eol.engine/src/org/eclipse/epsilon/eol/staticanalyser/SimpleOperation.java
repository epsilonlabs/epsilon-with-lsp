package org.eclipse.epsilon.eol.staticanalyser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.dom.TypeExpression;
import org.eclipse.epsilon.eol.staticanalyser.types.EolAnyType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class SimpleOperation implements IStaticOperation {
	private String name;
	private EolType contextType;
	private EolType returnType;
	private List<EolType> parameterTypes;
	
	public SimpleOperation(Operation op) {
		name = op.getName();
		contextType = (EolType) op.getData().get("contextType");
		returnType = (EolType) op.getData().get("returnType");
		this.parameterTypes = new ArrayList<EolType>();
		for (Parameter p: op.getFormalParameters()) {
			TypeExpression t = p.getTypeExpression();
			EolType parameterType = EolAnyType.Instance;
			if (t != null) {
				parameterType = (EolType) t.getData().get("resolvedType");
			}
			
			parameterTypes.add(parameterType);
		}
	}
	
	public SimpleOperation(String name, EolType contextType, EolType returnType, List<EolType> parameterTypes) {
		this.name = name;
		this.contextType = contextType;
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
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
	public List<EolType> getParameterTypes() {
		return parameterTypes;
	}

}
