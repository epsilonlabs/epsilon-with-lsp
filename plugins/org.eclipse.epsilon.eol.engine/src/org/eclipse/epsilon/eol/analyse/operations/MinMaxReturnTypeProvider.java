package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolNativeType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolUnionType;

public class MinMaxReturnTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		if (!contextType.isAssignableTo(EolNativeType.Number)
				|| !parameterTypes.get(0).isAssignableTo(EolNativeType.Number)) {
			return EolAnyType.Instance;
		}

		if (contextType.equals(parameterTypes.get(0))) {
			return contextType;
		} else {
			return new EolUnionType(EolPrimitiveType.Integer, EolPrimitiveType.Real);
		}
	}

}
