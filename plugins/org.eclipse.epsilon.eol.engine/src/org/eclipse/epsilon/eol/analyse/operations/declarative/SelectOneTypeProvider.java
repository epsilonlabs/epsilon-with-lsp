package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolType;

public class SelectOneTypeProvider implements IDeclarativeOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, EolType iteratorType, List<EolType> expressionType) {
		return iteratorType;
	}

}
