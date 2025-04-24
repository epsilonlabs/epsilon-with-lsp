package org.eclipse.epsilon.eol.m3;

import java.util.List;

public interface IMetaClass {
	List<IMetaClass> getSuperTypes();
	
	List<IMetaClass> getSubTypes();
	
	List<IProperty> getProperties();
	
	List<IProperty> getAllProperties();
	
	boolean isAbstract();
	
	void setAbstract(boolean isAbstract);
	
	IProperty getProperty(String name);
	
	String getName();
	
	IMetamodel getMetamodel();
}
