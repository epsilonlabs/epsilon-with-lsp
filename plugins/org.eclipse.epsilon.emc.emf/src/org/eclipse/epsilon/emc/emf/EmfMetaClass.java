package org.eclipse.epsilon.emc.emf;

import java.util.Objects;

import org.eclipse.epsilon.eol.m3.MetaClass;

public class EmfMetaClass extends MetaClass {

	public EmfMetaClass(String name, EmfModelMetamodel metamodel) {
		this.name = name;
		this.metamodel = metamodel;
	}
	
	public boolean equals(Object other) {
		if (!(other instanceof EmfMetaClass)){
			return false;
		}
		EmfMetaClass otherMetaClass = (EmfMetaClass) other;
		return this.name.equals(otherMetaClass.name) && this.metamodel.equals(otherMetaClass.metamodel);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, metamodel.hashCode());
	}
}
