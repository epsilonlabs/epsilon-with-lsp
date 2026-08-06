package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;

public class SelectOneTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionType) {
		return iteratorType;
	}

}
