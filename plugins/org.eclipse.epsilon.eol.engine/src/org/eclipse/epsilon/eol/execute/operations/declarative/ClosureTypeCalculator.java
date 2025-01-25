package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class ClosureTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		if (expressionTypes.get(0) instanceof EolCollectionType) {
			EolType contentType = ((EolCollectionType) expressionTypes.get(0)).getContentType();
			return new EolCollectionType("Sequence", contentType);
		}
		else {
			return new EolCollectionType("Sequence", expressionTypes.get(0));
		}
		
	}

}
