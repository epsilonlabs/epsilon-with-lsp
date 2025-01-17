package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class SortByTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		return new EolCollectionType("Sequence", iteratorType);
	}
}
