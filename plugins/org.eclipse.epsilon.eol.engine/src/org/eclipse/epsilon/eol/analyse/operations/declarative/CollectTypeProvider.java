package org.eclipse.epsilon.eol.analyse.operations.declarative;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;

public class CollectTypeProvider implements IDeclarativeOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, EolType iteratorType, List<EolType> expressionTypes) {
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
		return new EolCollectionType(newCollectionName, expressionTypes.get(0));
	}

}
