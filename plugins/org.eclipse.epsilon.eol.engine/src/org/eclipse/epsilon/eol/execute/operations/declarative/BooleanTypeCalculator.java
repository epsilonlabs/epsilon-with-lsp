package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.analyse.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.analyse.types.EolType;
import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;

public class BooleanTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		return EolPrimitiveType.Boolean;
	}

}
