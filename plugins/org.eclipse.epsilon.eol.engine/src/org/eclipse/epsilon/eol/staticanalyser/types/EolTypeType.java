package org.eclipse.epsilon.eol.staticanalyser.types;

public class EolTypeType extends EolType {
	private EolType wrappedType;

	public EolTypeType(EolType wrappedType){
		this.wrappedType = wrappedType;
	}
	
	@Override
	public String getName() {
		return "EolTypeType<" + wrappedType.getName() + ">";
	}
	
	public EolType getWrappedType() {
		return this.wrappedType;
	}

}
