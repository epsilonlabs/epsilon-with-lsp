package org.eclipse.epsilon.emc.emf;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.epsilon.eol.m3.IEnum;

public class EmfEnumMetaClass extends EmfMetaClass implements IEnum {

	public EmfEnumMetaClass(EEnum eEnum, EmfModelMetamodel metamodel) {
		super(eEnum, metamodel);
	}
	
	public boolean isValidEnumLiteral(String literal) {
		return ((EEnum)eClassifier).getEEnumLiteralByLiteral(literal) != null;
	}
}
