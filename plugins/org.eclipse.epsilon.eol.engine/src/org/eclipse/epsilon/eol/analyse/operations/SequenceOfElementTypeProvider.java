package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Returns a Sequence of the elements of the target collection, or of the target
 * itself if it is not a collection.
 * Used by operations such as <code>asSequence()</code>, <code>invert()</code>, <code>first(n)</code>.
 */
public class SequenceOfElementTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		return ElementTypes.collectionOf(EolCollectionType.Sequence, contextType);
	}

}
