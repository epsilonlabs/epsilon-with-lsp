package org.eclipse.epsilon.emc.emf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.epsilon.eol.m3.IEnum;

public class EmfEnumMetaClass extends EmfMetaClass implements IEnum {

	public EmfEnumMetaClass(EEnum eEnum, EmfModelMetamodel metamodel) {
		super(eEnum, metamodel);
	}
	
	public boolean isValidEnumLiteral(String literal) {
		return ((EEnum)eClassifier).getEEnumLiteralByLiteral(literal) != null;
	}

	@Override
	public List<String> getLiterals() {
		List<String> literals = new ArrayList<String>();
		for (EEnumLiteral literal : ((EEnum) eClassifier).getELiterals()) {
			literals.add(literal.getLiteral());
		}
		return literals;
	}
}
