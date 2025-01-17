package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolMapType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class MapByTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		return new EolMapType(expressionTypes.get(0), new EolCollectionType("Sequence", iteratorType));
	}

}
