package org.eclipse.epsilon.eol.analyse.operations.validation;

import java.util.List;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.m3.IMetaClass;
import org.eclipse.epsilon.eol.m3.UnknownMetamodel;
import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolMapType;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.eclipse.epsilon.eol.types.EolNativeType;
import org.eclipse.epsilon.eol.types.EolNoType;
import org.eclipse.epsilon.eol.types.EolTupleType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.eol.types.EolTypeLiteral;
import org.eclipse.epsilon.eol.types.EolUnionType;

/**
 * Checks shared by the validators that compare the arguments of an operation
 * call against the element types of the collection or map the operation is
 * invoked on.
 *
 * <p>Operations that store an argument (e.g. <code>add</code>,
 * <code>put</code>) require it to be assignable to the element type, and an
 * error is reported otherwise. Operations that only look an argument up (e.g.
 * <code>remove</code>, <code>includes</code>, <code>get</code>) accept any
 * argument whose type is related to the element type, and a warning is
 * reported otherwise, as such a call can never match anything.</p>
 */
final class ElementTypeChecks {

	private ElementTypeChecks() {}

	/**
	 * Reports an error if a value of the provided type cannot be stored where
	 * the expected type is required.
	 */
	static void checkAssignable(List<ModuleMarker> markers, AbstractModuleElement element,
			EolType parameterType, EolType expectedType) {
		if (!isAssignable(parameterType, expectedType)) {
			reportNotAssignable(markers, element, parameterType, expectedType);
		}
	}

	/**
	 * Reports an error if the elements of a collection of the provided type
	 * cannot be stored where the expected content type is required.
	 */
	static void checkContentAssignable(List<ModuleMarker> markers, AbstractModuleElement element,
			EolType parameterType, EolType expectedContentType) {
		EolType parameterContentType = getContentType(parameterType);
		if (parameterContentType != null && !isAssignable(parameterContentType, expectedContentType)) {
			reportNotAssignable(markers, element, parameterType,
					new EolCollectionType("Collection", expectedContentType));
		}
	}

	/**
	 * Reports an error if the entries of a map of the provided type cannot be
	 * stored where the expected key and value types are required.
	 */
	static void checkEntriesAssignable(List<ModuleMarker> markers, AbstractModuleElement element,
			EolType parameterType, EolType expectedKeyType, EolType expectedValueType) {
		EolType parameterKeyType = getKeyType(parameterType);
		EolType parameterValueType = getValueType(parameterType);
		if (parameterKeyType != null && parameterValueType != null
				&& (!isAssignable(parameterKeyType, expectedKeyType)
						|| !isAssignable(parameterValueType, expectedValueType))) {
			reportNotAssignable(markers, element, parameterType, new EolMapType(expectedKeyType, expectedValueType));
		}
	}

	private static void reportNotAssignable(List<ModuleMarker> markers, AbstractModuleElement element,
			EolType parameterType, EolType expectedType) {
		markers.add(new ModuleMarker(element, "Parameter Type " + parameterType
				+ " cannot be assigned to expected parameter type " + expectedType, Severity.Error));
	}

	/**
	 * Reports a warning if a value of the provided type can never be equal to
	 * a value of the expected type.
	 *
	 * @param role The role of the expected type, e.g. "contained", "key" or "value".
	 */
	static void checkComparable(List<ModuleMarker> markers, AbstractModuleElement element,
			EolType parameterType, EolType expectedType, String role) {
		if (!isComparable(parameterType, expectedType)) {
			markers.add(new ModuleMarker(element, "Parameter type " + parameterType + " is unrelated to "
					+ role + " type " + expectedType + ", so it can never match", Severity.Warning));
		}
	}

	/**
	 * Reports a warning if the elements of a collection of the provided type can
	 * never be equal to a value of the expected type.
	 */
	static void checkContentComparable(List<ModuleMarker> markers, AbstractModuleElement element,
			EolType parameterType, EolType expectedContentType) {
		EolType parameterContentType = getContentType(parameterType);
		if (parameterContentType != null && !isComparable(parameterContentType, expectedContentType)) {
			markers.add(new ModuleMarker(element, "Parameter content type " + parameterContentType
					+ " is unrelated to contained type " + expectedContentType + ", so it can never match", Severity.Warning));
		}
	}

	/**
	 * @return Whether a value of the first type could be of the second type, as
	 *         tested by e.g. <code>isKindOf(type)</code> or
	 *         <code>selectByKind(type)</code>. Two collection types, or two map
	 *         types, always could: those operations only compare their kinds,
	 *         and at run time a Sequence is a kind of Bag although the static
	 *         types are unrelated.
	 */
	static boolean canBeOfType(EolType valueType, EolType type) {
		if (isUnchecked(valueType) || isUnchecked(type)) {
			return true;
		}
		if (valueType instanceof EolUnionType) {
			return ((EolUnionType) valueType).containedTypes.stream().anyMatch(t -> canBeOfType(t, type));
		}
		if (type instanceof EolUnionType) {
			return ((EolUnionType) type).containedTypes.stream().anyMatch(t -> canBeOfType(valueType, t));
		}
		if (valueType instanceof EolCollectionType && type instanceof EolCollectionType
				|| isMapOrTuple(valueType) && isMapOrTuple(type)) {
			return true;
		}
		return isComparable(valueType, type);
	}

	private static boolean isMapOrTuple(EolType type) {
		return type instanceof EolMapType || type instanceof EolTupleType;
	}

	private static boolean isAssignable(EolType provided, EolType expected) {
		if (isUnchecked(provided) || isUnchecked(expected)) {
			return true;
		}
		if (provided instanceof EolUnionType) {
			return ((EolUnionType) provided).containedTypes.stream().allMatch(t -> isAssignable(t, expected));
		}
		return provided.isAssignableTo(expected);
	}

	private static boolean isComparable(EolType first, EolType second) {
		if (isUnchecked(first) || isUnchecked(second)) {
			return true;
		}
		if (first instanceof EolUnionType) {
			return ((EolUnionType) first).containedTypes.stream().anyMatch(t -> isComparable(t, second));
		}
		if (second instanceof EolUnionType) {
			return ((EolUnionType) second).containedTypes.stream().anyMatch(t -> isComparable(first, t));
		}
		return first.isAssignableTo(second) || second.isAssignableTo(first);
	}

	/**
	 * Types about which too little is known to report anything.
	 */
	private static boolean isUnchecked(EolType type) {
		if (type == null || EolNoType.Instance.equals(type)) {
			return true;
		}
		if (type instanceof EolNativeType && type.getClazz() == null) {
			return true;
		}
		if (type instanceof EolModelElementType) {
			IMetaClass metaClass = ((EolModelElementType) type).getMetaClass();
			return metaClass == null || metaClass.getMetamodel() instanceof UnknownMetamodel;
		}
		return false;
	}

	/**
	 * @return The content type of the given collection type, or null if it is
	 *         not a collection type.
	 */
	static EolType getContentType(EolType type) {
		return type instanceof EolCollectionType ? ((EolCollectionType) type).getContentType() : null;
	}

	/**
	 * @return The key type of the given map type, or null if it is not a map type.
	 */
	static EolType getKeyType(EolType type) {
		return type instanceof EolMapType ? ((EolMapType) type).getKeyType() : null;
	}

	/**
	 * @return The value type of the given map type, or null if it is not a map type.
	 */
	static EolType getValueType(EolType type) {
		return type instanceof EolMapType ? ((EolMapType) type).getValueType() : null;
	}

	/**
	 * @return The type named by the given type literal, or null if it is not a
	 *         type literal.
	 */
	static EolType getLiteralType(EolType type) {
		return type instanceof EolTypeLiteral ? ((EolTypeLiteral) type).getWrappedType() : null;
	}

	static EolType getFirstParameterType(List<EolType> parameterTypes) {
		return parameterTypes == null || parameterTypes.isEmpty() ? null : parameterTypes.get(0);
	}

	static EolType getLastParameterType(List<EolType> parameterTypes) {
		return parameterTypes == null || parameterTypes.isEmpty() ? null : parameterTypes.get(parameterTypes.size() - 1);
	}
}
