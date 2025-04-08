package org.eclipse.epsilon.eol.m3;

import java.util.List;

public interface IMetaClass {
	List<IMetaClass> getSuperTypes();
	
	List<IMetaClass> getSubTypes();
	
	List<Property> getProperties();
	
	List<Property> getAllProperties();
	
	boolean isAbstract();
	
	void setAbstract(boolean isAbstract);
	
	Property getProperty(String name);
	
	String getName();
	
	IMetamodel getMetamodel();
}
