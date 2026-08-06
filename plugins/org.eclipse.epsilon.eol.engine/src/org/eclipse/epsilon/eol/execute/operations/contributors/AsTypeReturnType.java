package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolTypeLiteral;
import org.eclipse.epsilon.eol.execute.operations.IMethodTypeCalculator;

public class AsTypeReturnType implements IMethodTypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
		if(parameterTypes.get(0) instanceof EolTypeLiteral) {
			return ((EolTypeLiteral) parameterTypes.get(0)).getWrappedType();
		}
		return EolAnyType.Instance;
	}

}
