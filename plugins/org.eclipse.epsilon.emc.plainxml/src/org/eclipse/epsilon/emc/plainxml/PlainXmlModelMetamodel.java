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
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PlainXmlModelMetamodel extends Metamodel {
	
	private boolean CONSOLE = false;
	
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
					addMetaClass(createSubClass(elementMetaClass));
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
			parentMetaClass.addProperty(collectionProperty);
		}
	}
	
	private void addNodeAttributes (NamedNodeMap nodeAttributes, PlainXmlMetaClass metaClass) {
		List<Node> listNodeAttributes = PlainXmlModelMetamodel.namedNodeMapToList(nodeAttributes);
		addNodeAttributes(listNodeAttributes, metaClass);
	}
		
	private void addNodeAttributes (List<Node> nodeAttributes, PlainXmlMetaClass metaClass) {
		for (Node attribute : nodeAttributes) {	
			metaClass.addProperty(createAnyProperty(attribute));
			metaClass.addProperty(createIntegerProperty(attribute));
		}
	}
	
	public boolean addMetaClass (PlainXmlMetaClass metaClass) {
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

		name = name.toLowerCase();
		if (name.startsWith("t_")) {
			name = name.substring(2);
		}

		IMetaClass iMetaClass = super.getMetaClass(name);
		PlainXmlMetaClass xmlMetaClass = getPlainXmlMetaClass(name);
		
		/*
		if (null == iMetaClass) {
			System.out.println("getMetaClass : " + name + " >> returning : null ");
		} else {
			System.out.println("getMetaClass : " + name + " >> returning : " + iMetaClass.getName() + " -- "
					+ xmlMetaClass.getName());
		}
		*/
		
		if (null != iMetaClass) {
			System.out.println("\ngetMetaClass(String " + name + ") " + iMetaClass.toString());
		}
		return iMetaClass;
	}
	
	
	public PlainXmlMetaClass getPlainXmlMetaClass(String metaClassName) {
		for (IMetaClass iMetaClass : metaClasses) {
			if (iMetaClass.getName().contains(metaClassName)) {
				return (PlainXmlMetaClass) iMetaClass;
			}
		}
		return null;
	}
	
	public PlainXmlMetaClass getPlainXMLMetaClass(Node node) {
		return getPlainXmlMetaClass(node.getNodeName());
	}
	
	public static List<Node> namedNodeMapToList (NamedNodeMap namedNodeMap) {
		ArrayList<Node> nodeList = new ArrayList<Node>();
		for (int i = 0; i < namedNodeMap.getLength(); i++ ) {
			nodeList.add(namedNodeMap.item(i));
		}		
		return nodeList;
	}
	
	public static List<Node> nodeListToList (NodeList nodeList) {		
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
		PlainXmlMetaClass element = new PlainXmlMetaClass("element", this);
		if (CONSOLE) {
			element.addProperty("text", EolPrimitiveType.String);
			element.addProperty("parentNode", EolAnyType.Instance);
			element.addProperty("children", EolAnyType.Instance);
		}
		return element;
	}
	
	private String formatClassName (String className) {
		return className.substring(0, 1).toUpperCase() + className.substring(1);
	}
	
	private PlainXmlMetaClass createPlainXmlMetaClass (Node node) {
		String nodeName = node.getNodeName();
		//String metaClassName = formatClassName(nodeName);

		String metaClassName = node.getNodeName();
		
		PlainXmlMetaClass metaClass =  new PlainXmlMetaClass(metaClassName, this);
		
		// link to the element class
		metaClass.addSuperType(elementClass);
		elementClass.addSubType(metaClass);

		return metaClass;		
	}
	
	private PlainXmlMetaClass createSubClass(PlainXmlMetaClass metaClass) {
		// Create subTypes t_ Class and link element and metaClass
		
		PlainXmlMetaClass subMetaClass = new PlainXmlMetaClass("t_" + metaClass.getName(), this);
		subMetaClass.addSuperType(elementClass);
		subMetaClass.addSuperType(metaClass);
		
		elementClass.addSubType(subMetaClass);
		metaClass.addSubType(subMetaClass);
		return subMetaClass;
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

	private IProperty createCollectionProperty (Node node) {
		return new IProperty() {
			
			@Override
			public EolType getType() {
				//EolCollectionType defaultCollection = EolCollectionType.Collection;
				EolCollectionType defaultCollection = EolCollectionType.Sequence;
				defaultCollection.setContentType(EolPrimitiveType.String);
				return defaultCollection;
			}
			
			@Override
			public String getName() {
				return "c_" + node.getNodeName();
			}
		};
	}
	

}
