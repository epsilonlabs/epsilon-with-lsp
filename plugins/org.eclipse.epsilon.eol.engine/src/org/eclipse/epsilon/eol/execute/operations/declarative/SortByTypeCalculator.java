package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;

public class SortByTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		return new EolCollectionType("Sequence", iteratorType);
	}
}
