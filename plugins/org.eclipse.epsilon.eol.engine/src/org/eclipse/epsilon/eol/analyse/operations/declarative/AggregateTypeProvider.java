package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolMapType;
import org.eclipse.epsilon.eol.types.EolType;

public class AggregateTypeProvider implements IDeclarativeOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		return new EolMapType(expressionTypes.get(0), expressionTypes.get(1));
	}

}
