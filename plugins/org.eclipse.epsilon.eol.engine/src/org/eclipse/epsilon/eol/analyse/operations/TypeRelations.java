package org.eclipse.epsilon.eol.analyse.operations;

import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolNoType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolUnionType;

/**
 * Helpers for type providers that compare the types of their target and arguments.
 */
final class TypeRelations {

	private TypeRelations() {}

	/**
	 * @return Whether nothing is known about the type.
	 */
	static boolean isUnknown(EolType type) {
		return type == null || type instanceof EolAnyType || EolNoType.Instance.equals(type);
	}

	/**
	 * @return Whether the type is known and every value of it can be assigned
	 *         to the target type.
	 */
	static boolean isKnownSubtype(EolType type, EolType target) {
		if (isUnknown(type)) {
			return false;
		}
		if (type instanceof EolUnionType) {
			return ((EolUnionType) type).containedTypes.stream().allMatch(t -> isKnownSubtype(t, target));
		}
		return type.isAssignableTo(target);
	}

	/**
	 * @return Whether the type is unknown or some value of it can be assigned
	 *         to the target type.
	 */
	static boolean maybeSubtype(EolType type, EolType target) {
		if (isUnknown(type)) {
			return true;
		}
		if (type instanceof EolUnionType) {
			return ((EolUnionType) type).containedTypes.stream().anyMatch(t -> maybeSubtype(t, target));
		}
		return type.isAssignableTo(target);
	}
}
