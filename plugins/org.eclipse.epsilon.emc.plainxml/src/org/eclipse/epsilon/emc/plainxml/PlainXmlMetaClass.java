package org.eclipse.epsilon.emc.plainxml;

import org.eclipse.epsilon.eol.m3.IMetaClass;
import org.eclipse.epsilon.eol.m3.IProperty;
import org.eclipse.epsilon.eol.m3.MetaClass;
import org.w3c.dom.Element;

public class PlainXmlMetaClass extends MetaClass {
	
	private String name;
	
	public PlainXmlMetaClass(String typeName, PlainXmlModelMetamodel metaModel) {
		super();
		this.name = typeName;
		this.metamodel = metaModel;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public Class<?> getClazz() {
		// THIS SHOULD RETURN W3C Element.class, which should match XMLmodel.root property type		
		//t_library cannot be assigned to Native<org.w3c.dom.Node>
		
		Class<Element> c = Element.class;		
		System.out.println(" [!] getClazz() " + this.getName() + " : " + c);
		return c;
	}
	

	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\nmetaModel: " + metamodel);
		sb.append("\nname: " + name);
		
		if (!properties.isEmpty()) {
			sb.append("\n properties : ");
			for (IProperty iProperty : properties) {
				sb.append("\n  - " + iProperty.getType() + " " + iProperty.getName());
			}
		}
		
		if (!superTypes.isEmpty()) {
			sb.append("\n superTypes :");
			for (IMetaClass superType : superTypes) {
				sb.append("\n  ^ " + superType.getName());
			}
		}

		if (!subTypes.isEmpty()) {
			sb.append("\n subTypes :");
			for (IMetaClass subType : subTypes) {
				sb.append("\n  _ " + subType.getName());
			}
		}
		return sb.toString();
	}

}
