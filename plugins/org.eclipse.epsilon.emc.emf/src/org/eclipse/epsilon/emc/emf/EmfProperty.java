package org.eclipse.epsilon.emc.emf;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.epsilon.common.util.StringUtil;
import org.eclipse.epsilon.eol.m3.IProperty;
import org.eclipse.epsilon.eol.m3.MetaClass;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;

public class EmfProperty implements IProperty {
	
	private EolType type;
	private String name;
	
	EmfProperty(EAttribute eAttribute) {
		this.name = eAttribute.getName();
		EolType featureType;
		String instanceClassName = eAttribute.getEAttributeType().getInstanceClassName();
		if (StringUtil.isOneOf(instanceClassName, String.class.getCanonicalName(), "String")) {
			featureType = EolPrimitiveType.String;
		} else if (StringUtil.isOneOf(instanceClassName, Integer.class.getCanonicalName(), "int")) {
			featureType = EolPrimitiveType.Integer;
		} else if (StringUtil.isOneOf(instanceClassName, Boolean.class.getCanonicalName(), "boolean")) {
			featureType = EolPrimitiveType.Boolean;
		} else if ((instanceClassName != null) && (instanceClassName.equals(Float.class.getCanonicalName())
				|| instanceClassName.equals(Double.class.getCanonicalName()))) {
			featureType = EolPrimitiveType.Real;
		} else
			featureType = EolAnyType.Instance;
		calculateType(eAttribute, featureType);
	}
	
	EmfProperty(EReference eReference, MetaClass referenceMetaClass) {
		this.name = eReference.getName();
		EolType featureType;
		if (referenceMetaClass != null) {
			featureType = new EolModelElementType(referenceMetaClass);
		}
		else {
			featureType = EolAnyType.Instance;
		}
		calculateType(eReference, featureType);
	}
	
	private void calculateType(EStructuralFeature eStructuralFeature, EolType featureType) {
		if (eStructuralFeature.isMany()) {
			String collectionTypeName;
			if (eStructuralFeature.isOrdered()) {
				collectionTypeName = eStructuralFeature.isUnique() ? "OrderedSet" : "Sequence";
			} else {
				collectionTypeName = eStructuralFeature.isUnique() ? "Set" : "Bag";
			}
			type =  new EolCollectionType(collectionTypeName, featureType);
		} else {
			type = featureType;
		}
	}
	
	@Override
	public EolType getType() {
		return this.type;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
