package org.eclipse.epsilon.eol.execute.operations.contributors;

import java.util.List;

import org.eclipse.epsilon.eol.execute.operations.IMethodTypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolAnyType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolTypeLiteral;

public class AsTypeReturnType implements IMethodTypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, List<EolType> parameterTypes) {
		if(parameterTypes.get(0) instanceof EolTypeLiteral) {
			return ((EolTypeLiteral) parameterTypes.get(0)).getWrappedType();
		}
		return EolAnyType.Instance;
	}

}
