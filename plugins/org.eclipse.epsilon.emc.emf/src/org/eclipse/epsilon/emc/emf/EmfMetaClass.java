package org.eclipse.epsilon.emc.emf;

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.epsilon.eol.m3.MetaClass;

public class EmfMetaClass extends MetaClass {
	protected EClass eClass;
	
	public EmfMetaClass(EClass eClass, EmfModelMetamodel metamodel) {
		this.eClass = Objects.requireNonNull(eClass, "eClass");
		this.metamodel = metamodel;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof EmfMetaClass)){
			return false;
		}
		EmfMetaClass otherMetaClass = (EmfMetaClass) other;
		return Objects.equals(getClassifierIdentity(), otherMetaClass.getClassifierIdentity())
			&& Objects.equals(metamodel, otherMetaClass.metamodel);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getClassifierIdentity(), metamodel);
	}

	@Override
	public String getName() {
		return eClass.getName();
	}
	
	public Class<?> getClazz(){
		Class<?> instanceClass = eClass.getInstanceClass();
		if (instanceClass != null) return instanceClass;
		else return EObject.class;
	}

	private String getClassifierIdentity() {
		return EcoreUtil.getURI(eClass).toString();
	}
}
