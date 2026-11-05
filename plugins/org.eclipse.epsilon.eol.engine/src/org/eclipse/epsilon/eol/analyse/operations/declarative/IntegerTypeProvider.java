package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;

public class IntegerTypeProvider implements IDeclarativeOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		return EolPrimitiveType.Integer;
	}

}
