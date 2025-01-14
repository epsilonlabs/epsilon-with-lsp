package org.eclipse.epsilon.eol.execute.operations.declarative;

import org.eclipse.epsilon.eol.execute.operations.ITypeCalculator;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class CollectTypeCalculator implements ITypeCalculator {

	@Override
	public EolType calculateType(EolType contextType, EolType iteratorType, EolType expressionType) {
		String collectionName = ((EolCollectionType)contextType).getName();
		String newCollectionName = null;
		if (collectionName.equals("Bag") || collectionName.equals("Set")) {
			newCollectionName = "Bag";
		}
		else if (collectionName.equals("Sequence") || collectionName.equals("OrderedSet")){
			newCollectionName = "Sequence";
		}
		else {
			throw new RuntimeException("Unknown collection name");
		}
		return new EolCollectionType(newCollectionName, expressionType);
	}

}
