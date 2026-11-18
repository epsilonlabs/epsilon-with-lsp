package org.eclipse.epsilon.eol.analyse.operations;

import java.util.List;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolTypeLiteral;

/**
 * Returns a collection of the same kind as the target that holds the type
 * passed as the argument, or the target's element type if that is more specific.
 * Used by <code>selectByKind(type)</code> and <code>selectByType(type)</code>.
 */
public class SelectByTypeReturnTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		EolType argumentType = parameterTypes.isEmpty() ? null : parameterTypes.get(0);
		if (!(contextType instanceof EolCollectionType) || !(argumentType instanceof EolTypeLiteral)) {
			return contextType;
		}
		EolCollectionType collectionType = (EolCollectionType) contextType;
		EolType selectedType = ((EolTypeLiteral) argumentType).getWrappedType();
		EolType elementType = collectionType.getContentType();
		if (TypeRelations.isKnownSubtype(elementType, selectedType)) {
			selectedType = elementType;
		}
		return new EolCollectionType(collectionType.getName(), selectedType);
	}

}
