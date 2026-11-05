package org.eclipse.epsilon.eol.analyse.operations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolUnionType;

/**
 * Mirrors CollectionUtil.flatten(): a collection with no nested collections
 * is returned as is, otherwise its contents are recursively flattened into a
 * new Sequence.
 */
public class FlattenReturnTypeProvider implements IOperationTypeProvider {

	@Override
	public EolType getType(EolType contextType, List<EolType> parameterTypes) {
		if (contextType instanceof EolCollectionType) {
			EolType contentType = ((EolCollectionType) contextType).getContentType();
			if (containsCollectionType(contentType)) {
				Set<EolType> types = new LinkedHashSet<>();
				collectFlattenedTypes(contentType, types);
				EolType flattenedType = types.size() == 1 ? types.iterator().next() : new EolUnionType(types);
				return new EolCollectionType("Sequence", flattenedType);
			}
		}
		return contextType;
	}

	protected boolean containsCollectionType(EolType type) {
		if (type instanceof EolUnionType) {
			return ((EolUnionType) type).containedTypes.stream().anyMatch(this::containsCollectionType);
		}
		return type instanceof EolCollectionType;
	}

	protected void collectFlattenedTypes(EolType type, Set<EolType> types) {
		if (type instanceof EolCollectionType) {
			collectFlattenedTypes(((EolCollectionType) type).getContentType(), types);
		}
		else if (type instanceof EolUnionType) {
			for (EolType containedType : ((EolUnionType) type).containedTypes) {
				collectFlattenedTypes(containedType, types);
			}
		}
		else {
			types.add(type);
		}
	}

}
