package org.eclipse.epsilon.eol.staticanalyser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.dom.TypeExpression;
import org.eclipse.epsilon.eol.execute.operations.MethodTypeCalculator;
import org.eclipse.epsilon.eol.execute.operations.MethodDiagnosticsCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolAnyType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class SimpleOperation implements IStaticOperation {
	private String name;
	private EolType contextType;
	private EolType returnType;
	private List<EolType> parameterTypes;
	private Optional<MethodTypeCalculator> methodTypeCalculator = Optional.empty();
	private Optional<MethodDiagnosticsCalculator> methodDiagnosticsCalculator = Optional.empty();

	public SimpleOperation(Operation op) {
		name = op.getName();
		contextType = (EolType) op.getData().get("contextType");
		returnType = (EolType) op.getData().get("returnType");
		this.parameterTypes = new ArrayList<EolType>();
		for (Parameter p : op.getFormalParameters()) {
			TypeExpression t = p.getTypeExpression();
			EolType parameterType = EolAnyType.Instance;
			if (t != null) {
				parameterType = (EolType) t.getData().get("resolvedType");
			}

			parameterTypes.add(parameterType);
		}
	}

	public SimpleOperation(String name, EolType contextType, EolType returnType, List<EolType> parameterTypes,
			Optional<MethodTypeCalculator> methodTypeCalculator,
			Optional<MethodDiagnosticsCalculator> methodDiagnosticsCalculator) {
		this.name = name;
		this.contextType = contextType;
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
		this.methodTypeCalculator = methodTypeCalculator;
		this.methodDiagnosticsCalculator = methodDiagnosticsCalculator;
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

	@Override
	public List<ModuleMarker> getExtraDiagnostics(AbstractModuleElement element, EolType actualContextType,
			List<EolType> actualParameterTypes) {
		if (methodDiagnosticsCalculator.isPresent()) {
			try {
				return methodDiagnosticsCalculator.get().klass().newInstance().calculateDiagnostics(element,
						actualContextType, actualParameterTypes);
			} catch (Exception e) {
				e.printStackTrace();
				return new ArrayList<ModuleMarker>();
			}
		} else {
			return new ArrayList<ModuleMarker>();
		}
	}

}
