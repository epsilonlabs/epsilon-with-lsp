package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Returns a OrderedSet of the elements of the target collection, or of the target
 * itself if it is not a collection.
 * Used by operations such as <code>asOrderedSet()</code>.
 */
public class OrderedSetOfElementTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		return ElementTypes.collectionOf(EolCollectionType.OrderedSet, contextType);
	}

}
