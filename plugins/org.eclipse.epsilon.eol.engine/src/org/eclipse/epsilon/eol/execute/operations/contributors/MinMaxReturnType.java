package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.List;

import org.eclipse.epsilon.eol.analyse.types.EolAnyType;
import org.eclipse.epsilon.eol.analyse.types.EolNativeType;
import org.eclipse.epsilon.eol.analyse.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.analyse.types.EolType;
import org.eclipse.epsilon.eol.analyse.types.EolUnionType;
import org.eclipse.epsilon.eol.execute.operations.IMethodTypeCalculator;

public class MinMaxReturnType implements IMethodTypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
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
