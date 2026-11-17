package org.eclipse.epsilon.eol.analyse.operations;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolNoType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolUnionType;

/**
 * Computes the type of the elements an operation sees when it treats its
 * target as a collection, e.g. <code>asSet()</code>, which returns the
 * elements of a collection, or a single value wrapped in a collection.
 */
final class ElementTypes {

	private ElementTypes() {}

	/**
	 * @return The content type of a collection type, the type itself for any
	 *         other type, and Any if nothing is known about the elements.
	 */
	static EolType of(EolType type) {
		if (type == null || type instanceof EolAnyType || EolNoType.Instance.equals(type)) {
			return EolAnyType.Instance;
		}
		if (type instanceof EolCollectionType) {
			return ((EolCollectionType) type).getContentType();
		}
		if (type instanceof EolUnionType) {
			Set<EolType> elementTypes = new LinkedHashSet<>();
			for (EolType containedType : ((EolUnionType) type).containedTypes) {
				elementTypes.add(of(containedType));
			}
			return elementTypes.size() == 1 ? elementTypes.iterator().next() : new EolUnionType(elementTypes);
		}
		Class<?> clazz = type.getClazz();
		if (clazz != null && (clazz.isArray() || Iterable.class.isAssignableFrom(clazz))) {
			// A native array or iterable, whose element type is not tracked
			return EolAnyType.Instance;
		}
		return type;
	}

	/**
	 * @return A collection type of the given kind, containing the elements of
	 *         the given type.
	 */
	static EolCollectionType collectionOf(EolCollectionType kind, EolType type) {
		return new EolCollectionType(kind.getName(), of(type));
	}
}
