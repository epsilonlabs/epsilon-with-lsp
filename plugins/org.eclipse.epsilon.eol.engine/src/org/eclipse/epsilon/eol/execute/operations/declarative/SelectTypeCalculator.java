package org.eclipse.epsilon.eol.execute.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.analyse.types.EolCollectionType;
import org.eclipse.epsilon.eol.analyse.types.EolType;
import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;

public class SelectTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
		String collectionName = ((EolCollectionType)contextType).getName();
		return new EolCollectionType(collectionName, iteratorType);
		
	}

}
