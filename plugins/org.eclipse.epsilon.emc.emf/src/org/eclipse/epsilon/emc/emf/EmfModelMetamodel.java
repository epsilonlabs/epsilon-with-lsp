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
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.m3.MetaClass;
import org.eclipse.epsilon.eol.m3.Metamodel;
import org.eclipse.epsilon.eol.m3.IProperty;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;

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
						EmfMetaClass metaClass = new EmfMetaClass(eClassifier, this);
						eClassMetaClassMap.put((EClass) eClassifier, metaClass);
						metaClasses.add(metaClass);
					}
					else if(eClassifier instanceof EEnum) {
						EmfMetaClass metaClass = new EmfEnumMetaClass((EEnum)eClassifier, this);
						metaClasses.add(metaClass);
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
						IProperty attribute = new EmfProperty(eAttribute);
						metaClass.getProperties().add(attribute);
					}
					
					for (EReference eReference : eClass.getEReferences()) {
						EClass referenceType = eReference.getEReferenceType();
						MetaClass referenceMetaClass = eClassMetaClassMap.get(referenceType);
						IProperty reference = new EmfProperty(eReference, referenceMetaClass);
						metaClass.getProperties().add(reference);
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
