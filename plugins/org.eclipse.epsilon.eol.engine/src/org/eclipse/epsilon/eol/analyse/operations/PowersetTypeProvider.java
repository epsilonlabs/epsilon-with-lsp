package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;

/**
 * Returns a set of sets of the elements of the target collection, as computed
 * by <code>powerset()</code>.
 */
public class PowersetTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		return new EolCollectionType(EolCollectionType.Set.getName(),
				ElementTypes.collectionOf(EolCollectionType.Set, contextType));
	}

}
