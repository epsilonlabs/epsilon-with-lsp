package org.eclipse.epsilon.eol.staticanalyser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.dom.TypeExpression;
import org.eclipse.epsilon.eol.execute.operations.MethodTypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolAnyType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class SimpleOperation implements IStaticOperation {
	private String name;
	private EolType contextType;
	private EolType returnType;
	private List<EolType> parameterTypes;
	private Optional<MethodTypeCalculator> methodTypeCalculator = Optional.empty();
	
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
	
	public SimpleOperation(String name, EolType contextType, EolType returnType, List<EolType> parameterTypes, MethodTypeCalculator methodTypeCalculator) {
		this.name = name;
		this.contextType = contextType;
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
		this.methodTypeCalculator = Optional.ofNullable(methodTypeCalculator);
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
	public EolType getReturnType(EolType actualContextType, List<EolType> actualParameterTypes) {
		if (methodTypeCalculator.isPresent()) {
			try {
				return methodTypeCalculator.get().klass().newInstance().calculateType(actualContextType,
						actualParameterTypes);
			} catch (Exception e) {
				e.printStackTrace();
				return returnType;
			}
		} else {
			return returnType;
		}
	}

	@Override
	public List<EolType> getParameterTypes() {
		return parameterTypes;
	}

}
