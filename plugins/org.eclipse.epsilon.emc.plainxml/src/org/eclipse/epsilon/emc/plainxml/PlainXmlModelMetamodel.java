package org.eclipse.epsilon.emc.plainxml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.m3.IMetaClass;
import org.eclipse.epsilon.eol.m3.IProperty;
import org.eclipse.epsilon.eol.m3.Metamodel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PlainXmlModelMetamodel extends Metamodel {
	
	private boolean CONSOLE = true;
	
	// Element Tag -- possible EClass/EFeature
	// Element Attributes -- possible EAttribute (Using XML attributes as references)
	// Element Text -- String Text (EAttribute)
	
	
	// Element -> NodeName -> t_NodeName
	
	/*
	 * 
	 * <Type1 Type2=value,Type2=value>Type3</Type1>
	 * 
	 * Model root [NodeType1]
	 * 
	 * NodeType1 - An XML Element
	 * 	- NodeType1	[Any] 		(child elements)
	 * 	- NodeType2 [Any] 		(element attribute)
	 * 	- NodeType3 [String]	(child texts)
	 * 
	 * Code convention to handle tags as:
	 * 	t_ = type
	 *		e_ = element
	 *  	c_ = collection
	 *  	x_ = reference
	 * 
	 * 	a_ = attribute (string)
	 * 		s_ = string (alias for a_)
	 * 		b_ = boolean
	 * 		r_ = real
	 * 		i_ = integer
	 * 
	 *  
	 *  .text = text for an element
	 *  .children = children of an element (combine with c_ or e_)
	 *  .parentNode = parent of an element 
	 *  
	 *  New elements, `new t_tagname`
	 *  Add child, `.appendChild()`
	 *  
	 *  Set the root element of a document (required) `XMLDoc.root = new t_library;`
	 *  
	 *  class t_+type1.name {
	 *  	attr Any a_+type2.name1;
	 *  	attr Any a_+type2.name2;
	 *  	
	 *  	val Any[] t_+type1.name1;
	 *  	val Any[] t_+type2.name1;
	 *  	
	 *  	val String[] type3.name;  // #text
	 *  
	 *  	attr Any parent;
	 *  	// val Any[] children;
	 *  
	 *  	val AnyChild children;
	 *  }
	 *  
	 *  class anyChild {
	 *  	// one attribute for every child element attribute
	 *  	attr Any a_+type2.name1
	 *  	attr Any a_+type2.name2
	 *  }
	 *  
	 *  class node {
	 *  	attr Any a_+type2.name1
	 *  }
	 *  
	 *  
	 *  
	 *  
	 *  Binds create references, model.bind(sourceTagName, sourceTagAttribute, targetTagName, targetTagAttribute
	 *  
	 *  
	 */
	
	
	// protected List<IMetaClass> metaClasses = new ArrayList<>();
	
	
	protected StringProperties properties;
	IRelativePathResolver resolver;
	protected String modelName;
	protected PlainXmlModel model;
	
	private PlainXmlMetaClass elementClass = createElementClass();
	
	public PlainXmlModelMetamodel(PlainXmlModel model, StringProperties properties, IRelativePathResolver resolver,
			String modelName) {
		super();
		
		createSyntheticMetamodel(model);
	}
	
	private void createSyntheticMetamodel(PlainXmlModel model) {
		Collection<Element> xmlContent = model.allContentsFromModel();		
		
		addMetaClass(elementClass);
		
		// All the other nodes
		for (Element element : xmlContent) {
			
			// Only create Classes for Type1 nodes with a null value
			if(element.getNodeType() == 1 && element.getNodeValue() == null) {

				PlainXmlMetaClass elementMetaClass = getPlainXMLMetaClass(element);
				
				if(null == elementMetaClass) {
					elementMetaClass = createPlainXmlMetaClass(element);
					addMetaClass(elementMetaClass);
				}
				//elementMetaClass.addNode(element);
				
				// Update Meta Class properties 				
				addNodeAttributes(element.getAttributes(), elementMetaClass);
				 				 
				// Add collections to Meta class
				addCollectionPropertyToParent(element.getParentNode(), element);
			}
		}
		
		if (CONSOLE) {
			System.out.println("\n\n META MODEL");
			int index = 0;
			for (IMetaClass metaClass : metaClasses) {
				System.out.println("[" + index + "] " + metaClass);
				index++;
			}
		}
	}
	
	private void addCollectionPropertyToParent (Node parent, Node child) {
		//String elementPropertyName = "e_" + child.getNodeName();
		IProperty collectionProperty = createCollectionProperty(child);
		PlainXmlMetaClass parentMetaClass = getPlainXMLMetaClass(parent);
		if(null != parentMetaClass) {
			//parentMetaClass.addProperty(collectionProperty);
			addMetaClassProperty(parentMetaClass, collectionProperty);
		}
	}
	
	private void addNodeAttributes (NamedNodeMap nodeAttributes, PlainXmlMetaClass metaClass) {
		List<Node> listNodeAttributes = namedNodeMapToList(nodeAttributes);
		addNodeAttributes(listNodeAttributes, metaClass);
	}
		
	private void addNodeAttributes (List<Node> nodeAttributes, PlainXmlMetaClass metaClass) {
		for (Node attribute : nodeAttributes) {	
			
			// TODO Clever attribute to property type creation

			// We could get the value of the attributes and try to make a guess a what property types to create
			// However, we may need to update property types if we discover another node with the same attribute (which is more/less restrictive)
			// String value = attribute.getNodeValue();
			
			// Every thing text so a type of Any and String
			addMetaClassProperty(metaClass, createAnyProperty(attribute));
			addMetaClassProperty(metaClass, createStringProperty(attribute));
			
			// Some things are numbers Integers or Reals (1 or 1.0)
			if(true) {
				addMetaClassProperty(metaClass, createIntegerProperty(attribute));
				addMetaClassProperty(metaClass, createRealProperty(attribute));
			}

			// Very few things would be Booleans (True/False or true/false)
			if(true) {
				addMetaClassProperty(metaClass, createBooleanProperty(attribute));
			}
		}
	}
	
	private boolean addMetaClass (PlainXmlMetaClass metaClass) {
		// Do not duplicate metaClasses
		for (IMetaClass iMetaClass : metaClasses) {
			if(iMetaClass.getName().contains(metaClass.getName())) {
				return false;
			}
		}
		
		this.metaClasses.add(metaClass);
		return true;
	}
	
	@Override
	public IMetaClass getMetaClass(String name) {
		IMetaClass iMetaClass = super.getMetaClass(name);
		if (CONSOLE && null != iMetaClass) {
			System.out.println("\ngetMetaClass(String " + name + ")\n " + iMetaClass.toString() + "\n");
		}
		return iMetaClass;
	}
	
	private PlainXmlMetaClass getPlainXmlMetaClass(String metaClassName) {
		for (IMetaClass iMetaClass : metaClasses) {
			if (iMetaClass.getName().contains(metaClassName)) {
				return (PlainXmlMetaClass) iMetaClass;
			}
		}
		return null;
	}
	
	private PlainXmlMetaClass getPlainXMLMetaClass(Node node) {
		return getPlainXmlMetaClass(node.getNodeName());
	}
	
	private List<Node> namedNodeMapToList (NamedNodeMap namedNodeMap) {
		ArrayList<Node> nodeList = new ArrayList<Node>();
		for (int i = 0; i < namedNodeMap.getLength(); i++ ) {
			nodeList.add(namedNodeMap.item(i));
		}		
		return nodeList;
	}
	
	// TODO remove this method if it is not used
	private List<Node> nodeListToList (NodeList nodeList) {		
		ArrayList<Node> list = new ArrayList<Node>();
		for (int i = 0; i < nodeList.getLength(); i++ ) {
			list.add(nodeList.item(i));
		}		
		return list;
	}
	
	@Override
	public boolean equals(Object other) {
		if(other == this) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// Needs a MetaClass instance called "Element" which is the parent of all the XML elements
	
	private PlainXmlMetaClass createElementClass() {
		PlainXmlMetaClass element = new PlainXmlMetaClass("Element", this);
		addMetaClassProperty(element, "text", EolPrimitiveType.String);
		addMetaClassProperty(element, "parentNode", EolAnyType.Instance);
		addMetaClassProperty(element, "children", EolAnyType.Instance);
		return element;
	}
	
	private PlainXmlMetaClass createPlainXmlMetaClass (Node node) {
		String metaClassName = "t_" + node.getNodeName();
		PlainXmlMetaClass metaClass =  new PlainXmlMetaClass(metaClassName, this);

		// link to the element class
		addMetaClassSuperType(metaClass, elementClass);
		addMetaClassSubType(elementClass, metaClass);
		return metaClass;		
	}
		
	private IProperty createProperty (String name, EolType type) {
		return new IProperty() {
			
			@Override
			public EolType getType() {
				return type;
			}
			
			@Override
			public String getName() {
				return name;
			}
		};
	}
	
	private IProperty createAnyProperty (Node node) {
		return new IProperty() {			
			@Override
			public EolType getType() {				
				return EolAnyType.Instance;
			}
			
			@Override
			public String getName() {
				return "a_" + node.getNodeName();
			}
		};
	}
	
	private IProperty createStringProperty (Node node) {
		return new IProperty() {			
			@Override
			public EolType getType() {				
				return EolPrimitiveType.String;
			}
			
			@Override
			public String getName() {
				return "s_" + node.getNodeName();
			}
		};
	}
	
	private IProperty createIntegerProperty (Node node) {
		return new IProperty() {			
			@Override
			public EolType getType() {				
				return EolPrimitiveType.Integer;
			}
			
			@Override
			public String getName() {
				return "i_" + node.getNodeName();
			}
		};
	}
	
	private IProperty createRealProperty (Node node) {
		return new IProperty() {			
			@Override
			public EolType getType() {				
				return EolPrimitiveType.Real;
			}
			
			@Override
			public String getName() {
				return "r_" + node.getNodeName();
			}
		};
	}
	
	private IProperty createBooleanProperty (Node node) {
		return new IProperty() {			
			@Override
			public EolType getType() {				
				return EolPrimitiveType.Boolean;
			}
			
			@Override
			public String getName() {
				return "b_" + node.getNodeName();
			}
		};
	}

	private IProperty createCollectionProperty(Node node) {

		PlainXmlMetaClass type = getPlainXMLMetaClass(node);

		if (null != type) {
			return new IProperty() {
				@Override
				public EolType getType() {
					EolCollectionType defaultCollection = EolCollectionType.Sequence;
					defaultCollection.setContentType(new EolModelElementType(type));
					return defaultCollection;
				}

				@Override
				public String getName() {
					return "c_" + node.getNodeName();
				}
			};
		}
		else {
			//TODO handle creating a collection when the type (t_) is unknown.
			// Another option here would be to create a new t_ class
			System.err.println("Created Collection<Any> for unknown type in private IProperty createCollectionProperty(Node node) -- node name : " + node.getNodeName());
			return new IProperty() {
				@Override
				public EolType getType() {
					EolCollectionType defaultCollection = EolCollectionType.Sequence;
					defaultCollection.setContentType(EolAnyType.Instance);
					return defaultCollection;
				}

				@Override
				public String getName() {
					return "c_" + node.getNodeName();
				}
			};
		}
	}
	
	private boolean addMetaClassProperty(PlainXmlMetaClass metaClass, IProperty property) {
		IProperty p = metaClass.getProperty(property.getName());
		if (null == p) {
			metaClass.getProperties().add(property);
			return true;
		}
		return false;
	}

	private boolean addMetaClassProperty(PlainXmlMetaClass metaClass, String propertyName, EolType type) {
		IProperty property = metaClass.getProperty(propertyName);
		if (null == property) {
			property = createProperty(propertyName, type);
			metaClass.getProperties().add(property);
			System.out.println("Added property: " + property.getName() + " on " + metaClass.getName());
			return true;
		} 
		System.out.println("Dupe property: " + propertyName);
		return false;
	}
	
	
	private boolean addMetaClassSubType(PlainXmlMetaClass metaClass, IMetaClass subMetaClass) {
		for (IMetaClass type : metaClass.getSubTypes()) {
			if(type.getName().equals(subMetaClass.getName())) {
				return false;
			}
		}
		metaClass.getSubTypes().add(subMetaClass);
		return true;
	}

	private boolean addMetaClassSuperType(PlainXmlMetaClass metaClass, IMetaClass superMetaClass) {
		for(IMetaClass type : metaClass.getSuperTypes()) {
			if(type.getName().equals(superMetaClass.getName())) {
				return false;
			}
		}
		
		metaClass.getSuperTypes().add(superMetaClass);
		return true;
	}
	
	
	
}
