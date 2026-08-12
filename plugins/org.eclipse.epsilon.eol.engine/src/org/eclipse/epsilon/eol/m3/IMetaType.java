package org.eclipse.epsilon.eol.m3;

public interface IMetaType {

	String getName();

	IMetamodel getMetamodel();

	Class<?> getClazz();
}
