package org.eclipse.epsilon.eol.analyse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.analyse.operations.OperationTypeProvider;
import org.eclipse.epsilon.eol.analyse.operations.validation.OperationCallValidator;
import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.dom.TypeExpression;

public class SimpleOperation implements IStaticOperation {
	private String name;
	private EolType contextType;
	private EolType returnType;
	private List<EolType> parameterTypes;
	private List<String> parameterNames;
	private boolean varArgs;
	private Method method;
	private Operation operation;
	private Optional<OperationTypeProvider> operationTypeProvider = Optional.empty();
	private Optional<OperationCallValidator> operationCallValidator = Optional.empty();

	public SimpleOperation(Operation op) {
		operation = op;
		name = op.getName();
		contextType = (EolType) op.getData().get("contextType");
		returnType = (EolType) op.getData().get("returnType");
		this.parameterTypes = new ArrayList<EolType>();
		this.parameterNames = new ArrayList<String>();
		this.varArgs = false;
		for (Parameter p : op.getFormalParameters()) {
			TypeExpression t = p.getTypeExpression();
			EolType parameterType = EolAnyType.Instance;
			if (t != null) {
				parameterType = (EolType) t.getData().get("resolvedType");
			}

			parameterTypes.add(parameterType);
			parameterNames.add(p.getName());
		}
	}

	public SimpleOperation(String name, EolType contextType, EolType returnType, List<EolType> parameterTypes,
			Optional<OperationTypeProvider> operationTypeProvider,
			Optional<OperationCallValidator> operationCallValidator) {
		this(name, contextType, returnType, parameterTypes, false, operationTypeProvider, operationCallValidator);
	}

	public SimpleOperation(String name, EolType contextType, EolType returnType, List<EolType> parameterTypes,
			boolean varArgs, Optional<OperationTypeProvider> operationTypeProvider,
			Optional<OperationCallValidator> operationCallValidator) {
		this(name, contextType, returnType, parameterTypes, defaultParameterNames(parameterTypes), varArgs,
				operationTypeProvider, operationCallValidator, null);
	}

	public SimpleOperation(String name, EolType contextType, EolType returnType, List<EolType> parameterTypes,
			List<String> parameterNames, boolean varArgs, Optional<OperationTypeProvider> operationTypeProvider,
			Optional<OperationCallValidator> operationCallValidator, Method method) {
		this.name = name;
		this.contextType = contextType;
		this.returnType = returnType;
		this.parameterTypes = parameterTypes;
		this.parameterNames = normaliseParameterNames(parameterTypes, parameterNames);
		this.varArgs = varArgs;
		this.operationTypeProvider = operationTypeProvider;
		this.operationCallValidator = operationCallValidator;
		this.method = method;
	}

	public SimpleOperation(String name, EolType contextType, EolType returnType, List<EolType> parameterTypes,
			boolean varArgs, Optional<OperationTypeProvider> operationTypeProvider,
			Optional<OperationCallValidator> operationCallValidator, Method method) {
		this(name, contextType, returnType, parameterTypes, defaultParameterNames(parameterTypes), varArgs,
				operationTypeProvider, operationCallValidator, method);
	}

	private static List<String> defaultParameterNames(List<EolType> parameterTypes) {
		List<String> names = new ArrayList<String>();
		int size = parameterTypes != null ? parameterTypes.size() : 0;
		for (int i = 0; i < size; i++) {
			names.add("arg" + i);
		}
		return names;
	}

	private static List<String> normaliseParameterNames(List<EolType> parameterTypes, List<String> parameterNames) {
		List<String> names = new ArrayList<String>();
		int size = parameterTypes != null ? parameterTypes.size() : 0;
		for (int i = 0; i < size; i++) {
			String name = parameterNames != null && i < parameterNames.size() ? parameterNames.get(i) : null;
			names.add(name != null && !name.isEmpty() ? name : "arg" + i);
		}
		return names;
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
		if (operationTypeProvider.isPresent()) {
			try {
				return operationTypeProvider.get().value().newInstance().getType(actualContextType,
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
	public List<String> getParameterNames() {
		return parameterNames;
	}

	@Override
	public boolean isVarArgs() {
		return varArgs;
	}

	public Method getMethod() {
		return method;
	}

	public Operation getOperation() {
		return operation;
	}

	@Override
	public List<ModuleMarker> validateCall(AbstractModuleElement element, EolType actualContextType,
			List<EolType> actualParameterTypes) {
		if (operationCallValidator.isPresent()) {
			try {
				return operationCallValidator.get().value().newInstance().validate(element,
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
