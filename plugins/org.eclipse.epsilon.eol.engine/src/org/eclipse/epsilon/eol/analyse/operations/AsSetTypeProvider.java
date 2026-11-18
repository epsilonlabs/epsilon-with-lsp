package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Returns a Set of the elements of the target collection, or of the target
 * itself if it is not a collection.
 * Used by <code>asSet()</code>.
 */
public class AsSetTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		return ElementTypes.collectionOf(EolCollectionType.Set, contextType);
	}

}
