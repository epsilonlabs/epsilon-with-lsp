package org.eclipse.epsilon.eol.m3;

import java.util.List;

public interface IMetaClass {
	List<IMetaClass> getSuperTypes();
	
	List<IMetaClass> getSubTypes();
	
	List<StructuralFeature> getStructuralFeatures();
	
	List<StructuralFeature> getAllStructuralFeatures();
	
	boolean isAbstract();
	
	void setAbstract(boolean isAbstract);
	
	StructuralFeature getStructuralFeature(String name);
	
	String getName();
	
	IMetamodel getMetamodel();
}
