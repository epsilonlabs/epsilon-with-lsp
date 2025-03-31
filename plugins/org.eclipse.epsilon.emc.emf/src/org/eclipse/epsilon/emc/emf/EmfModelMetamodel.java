/*********************************************************************
* Copyright (c) 2008 The University of York.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.epsilon.emc.emf;

import java.util.HashMap;
import java.util.Objects;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.common.util.StringUtil;
import org.eclipse.epsilon.eol.m3.Attribute;
import org.eclipse.epsilon.eol.m3.MetaClass;
import org.eclipse.epsilon.eol.m3.Metamodel;
import org.eclipse.epsilon.eol.m3.Reference;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;

public class EmfModelMetamodel extends Metamodel {
	protected String nsuri;
	
	public String getNsURI() {
		return nsuri;
	}
	
	public EmfModelMetamodel(StringProperties properties, IRelativePathResolver resolver, String modelName) {
		
		HashMap<EClass, MetaClass> eClassMetaClassMap = new HashMap<>();
		nsuri = properties.getProperty("nsuri");
		if (nsuri == null) {
			getErrors().add("Required property nsuri not found");
		}
		else {
			EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(nsuri);
			if (ePackage == null) {
				getErrors().add("EPackage with nsURI " + nsuri + " is not available in EPackage.Registry.INSTANCE");
			}
			else {
				for (EClassifier eClassifier : ePackage.getEClassifiers()) {
					if (eClassifier instanceof EClass) {
						EmfMetaClass metaClass = new EmfMetaClass(eClassifier.getName(), this);
						eClassMetaClassMap.put((EClass) eClassifier, metaClass);
						metaTypes.add(metaClass);
					}
				}
				for (EClass eClass : eClassMetaClassMap.keySet()) {
					MetaClass metaClass = eClassMetaClassMap.get(eClass);
					for (EClass eSuperType : eClass.getESuperTypes()) {
						MetaClass superType = eClassMetaClassMap.get(eSuperType);
						if (superType != null) metaClass.getSuperTypes().add(superType);
					}
					
					for (EClass possibleSubClass : eClassMetaClassMap.keySet()) {
						if (eClass.isSuperTypeOf(possibleSubClass) && !possibleSubClass.equals(eClass)) {
							MetaClass subType = eClassMetaClassMap.get(possibleSubClass);
							if (subType != null) metaClass.getSubTypes().add(subType);
						}
					}
					
					
					for (EAttribute eAttribute : eClass.getEAttributes()) {
						Attribute attribute = new Attribute();
						attribute.setName(eAttribute.getName());
						attribute.setOrdered(eAttribute.isOrdered());
						attribute.setUnique(eAttribute.isUnique());
						attribute.setMany(eAttribute.isMany());
						
						String instanceClassName = eAttribute.getEAttributeType().getInstanceClassName();
						if (StringUtil.isOneOf(instanceClassName, String.class.getCanonicalName(), "String")) {
							attribute.setType(EolPrimitiveType.String);
						}
						else if (StringUtil.isOneOf(instanceClassName, Integer.class.getCanonicalName(), "int")) {
							attribute.setType(EolPrimitiveType.Integer);
						}
						else if (StringUtil.isOneOf(instanceClassName, Boolean.class.getCanonicalName(), "boolean")) {
							attribute.setType(EolPrimitiveType.Boolean);
						}
						else if ((instanceClassName != null) && (instanceClassName.equals(Float.class.getCanonicalName()) || instanceClassName.equals(Double.class.getCanonicalName()))) {
							attribute.setType(EolPrimitiveType.Real);
						}
						else
							attribute.setType(EolAnyType.Instance);
						metaClass.getStructuralFeatures().add(attribute);
					}
					
					for (EReference eReference : eClass.getEReferences()) {
						Reference reference = new Reference();
						reference.setName(eReference.getName());
						reference.setOrdered(eReference.isOrdered());
						reference.setUnique(eReference.isUnique());
						reference.setMany(eReference.isMany());
						reference.setContainment(eReference.isContainment());
						
						EClass referenceType = eReference.getEReferenceType();
						MetaClass referenceMetaClass = eClassMetaClassMap.get(referenceType);
						if (referenceMetaClass != null) {
							reference.setType(new EolModelElementType(referenceMetaClass));
						}
						metaClass.getStructuralFeatures().add(reference);
					}
					
					 metaClass.setAbstract(eClass.isAbstract());
				}
			}
		}
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof EmfModelMetamodel)){
			return false;
		}
		EmfModelMetamodel otherMetamodel = (EmfModelMetamodel) other;
		return this.getNsURI().equals(otherMetamodel.getNsURI());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(nsuri, getClass().getName());
	}
	
}
