package org.eclipse.epsilon.eol.staticanalyser.types;

public class EolTypeLiteral extends EolType {
	private EolType wrappedType;

	public EolTypeLiteral(EolType wrappedType){
		this.wrappedType = wrappedType;
	}
	
	@Override
	public String getName() {
		return "EolTypeLiteral<" + wrappedType.getName() + ">";
	}
	
	public EolType getWrappedType() {
		return this.wrappedType;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof EolTypeLiteral;
	}

}
