package org.eclipse.epsilon.emc.plainxml;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.epsilon.eol.m3.IMetaClass;
import org.eclipse.epsilon.eol.m3.IProperty;
import org.eclipse.epsilon.eol.m3.MetaClass;
import org.eclipse.epsilon.eol.types.EolType;
import org.w3c.dom.Element;

public class PlainXmlMetaClass extends MetaClass {
	
	

	private String name;
	//private UUID uuid = UUID.randomUUID();
	//protected List<Node> nodes = new ArrayList<Node>();
	
	public PlainXmlMetaClass(String typeName, PlainXmlModelMetamodel metaModel) {
		super();
		this.name = typeName;
		this.metamodel = metaModel;
	}

	/*
	public void addNode(Node node) {
		nodes.add(node);
	}
	*/
	
	public void propertiesToConsole () {
		System.out.println("MetaClass: " + getName());
		for (IProperty iProperty : getAllProperties()) {
			System.out.println(" - " + iProperty.toString());
		}
	}

	public boolean addSuperType(IMetaClass metaClass) {
		for(IMetaClass type : superTypes) {
			if(type.getName().equals(metaClass.getName())) {
				return false;
			}
		}
		
		superTypes.add(metaClass);
		return true;
	}
	
	public boolean addSubType(IMetaClass subType) {
		for (IMetaClass type : subTypes) {
			if(type.getName().equals(subType.getName())) {
				return false;
			}
		}
		this.subTypes.add(subType);
		return true;
	}

	
	public boolean addProperty(IProperty property) {
		IProperty p = getProperty(property.getName());
		if (null == p) {
			properties.add(property);
			return true;
		}
		return false;
	}
	
	public boolean addProperty(String name, EolType type) {
		IProperty p = getProperty(name);
		
		if (null == p) {
			p = new IProperty() {
				
				@Override
				public EolType getType() {
					return type;
				}
				
				@Override
				public String getName() {
					return name;
				}
			};
			properties.add(p);	
			System.out.println("Added property: " + p.getName() + " on " + this.getName());
			return true;
		} 
		
		System.out.println("Dupe property: " + p.getName());
		return false;
	}

	@Override
	public String getName() {
		return name;
	}
	
	/*
	public String getUuidString() {
		return uuid.toString();
	}
	*/
	
	@Override
	public Class<?> getClazz() {
		//t_library cannot be assigned to Native<org.w3c.dom.Node>
		
		
		Class<Element> c = Element.class;		
		System.out.println(" [!] getClazz() " + this.getName() + " : " + c);
		return c;
		//return Element.class;
	}
	
	@Override
	public boolean equals(Object other) {

		if (other == this) {
			System.out.println(" == matched");
			return true;
		}
		System.out.println(" != matched");
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
//		sb.append("\n UUID: " + uuid);
		
		if (!properties.isEmpty()) {
			sb.append("\n properties : ");
			for (IProperty iProperty : properties) {
				sb.append("\n  - " + iProperty.getType() + " " + iProperty.getName());
			}
		}
		
		/*
		if(!nodes.isEmpty()) {
			sb.append("\n nodes :");
			for ( Node node : nodes) {
				sb.append("\n  * " + node);
			}
		}
		*/
		
		if (!superTypes.isEmpty()) {
			sb.append("\n superTypes :");
			for (IMetaClass superType : superTypes) {
				sb.append("\n  ^ " + superType.getName());
				//sb.append("\n  ^ " + superType.getName() + " " + ((PlainXmlMetaClass) superType).getUuidString());
			}
		}

		if (!subTypes.isEmpty()) {
			sb.append("\n subTypes :");
			for (IMetaClass subType : subTypes) {
				sb.append("\n  _ " + subType.getName());
				//sb.append("\n  _ " + subType.getName() + " " + ((PlainXmlMetaClass) subType).getUuidString());
			}
		}
		return sb.toString();
	}

}
