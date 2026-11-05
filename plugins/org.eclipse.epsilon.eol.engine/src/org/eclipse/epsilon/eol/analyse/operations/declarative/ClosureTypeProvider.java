package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;

public class ClosureTypeProvider implements IDeclarativeOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		if (expressionTypes.get(0) instanceof EolCollectionType) {
			EolType contentType = ((EolCollectionType) expressionTypes.get(0)).getContentType();
			return new EolCollectionType("Sequence", contentType);
		}
		else {
			return new EolCollectionType("Sequence", expressionTypes.get(0));
		}
		
	}

}
