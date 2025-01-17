package org.eclipse.epsilon.eol.execute.operations.declarative;

import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolMapType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class MapByTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, EolType expressionType) {
		return new EolMapType(expressionType, new EolCollectionType("Sequence", iteratorType));
	}

}
