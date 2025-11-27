package org.eclipse.epsilon.emc.emf;

import java.util.Objects;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.epsilon.eol.m3.MetaClass;

public class EmfMetaClass extends MetaClass {
	protected EClassifier eClassifier;
	
	public EmfMetaClass(EClassifier eClassifier, EmfModelMetamodel metamodel) {
		this.eClassifier = eClassifier;
		this.metamodel = metamodel;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof EmfMetaClass)){
			return false;
		}
		EmfMetaClass otherMetaClass = (EmfMetaClass) other;
		return this.getName().equals(otherMetaClass.getName()) && this.metamodel.equals(otherMetaClass.metamodel);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getName(), metamodel);
	}

	@Override
	public String getName() {
		return this.eClassifier.getName();
	}
	
	public Class<?> getClazz(){
		return eClassifier.getInstanceClass();
	}
}
