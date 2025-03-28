package org.eclipse.epsilon.eol.m3;

import java.util.List;

public interface IMetamodel {
	String getNsURI();

	void setNsURI(String nsURI);

	List<String> getWarnings();

	List<String> getErrors();

	IMetaClass getMetaClass(String name);

	List<Package> getSubPackages();

	List<MetaType> getTypes();
}
