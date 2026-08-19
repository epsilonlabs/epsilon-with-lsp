/*******************************************************************************
 * Copyright (c) 2026 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.epsilon.emc.plainxml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.common.util.StringUtil;
import org.eclipse.epsilon.eol.m3.IProperty;
import org.eclipse.epsilon.eol.m3.Metamodel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.types.EolCollectionType;
import org.eclipse.epsilon.eol.types.EolModelElementType;
import org.eclipse.epsilon.eol.types.EolNativeType;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.types.EolType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PlainXmlModelMetamodel extends Metamodel {

	protected final String source;

	public PlainXmlModelMetamodel(StringProperties properties, IRelativePathResolver resolver, String modelName) {
		String example = properties.getProperty(PlainXmlModel.PROPERTY_EXAMPLE);
		String resolvedSource;
		try {
			resolvedSource = getSource(example, resolver, modelName);
		}
		catch (RuntimeException ex) {
			resolvedSource = "model:" + modelName;
			getErrors().add("Could not resolve Plain XML example: " + ex.getMessage());
		}
		source = resolvedSource;

		PlainXmlMetaClass elementClass = new PlainXmlMetaClass(PlainXmlModel.ELEMENT_TYPE, this);
		getTypes().add(elementClass);
		addCoreProperties(elementClass);
		if (!getErrors().isEmpty()) {
			return;
		}

		if (StringUtil.isEmpty(example)) {
			getErrors().add("Required property example not found");
			return;
		}

		try {
			inferTypes(parseExample(example, resolver), elementClass);
		}
		catch (Exception ex) {
			getErrors().add("Error whilst loading Plain XML example for model " + modelName + ": " + ex.getMessage());
		}
	}

	private Collection<Element> parseExample(String example, IRelativePathResolver resolver) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		factory.setExpandEntityReferences(false);

		String resolved = resolver != null ? resolver.resolve(example) : example;
		Document document = factory.newDocumentBuilder().parse(new File(resolved));

		Collection<Element> elements = new ArrayList<>();
		collectElements(document, elements);
		return elements;
	}

	private void collectElements(Node node, Collection<Element> elements) {
		if (node instanceof Element) {
			elements.add((Element) node);
		}
		NodeList children = node.getChildNodes();
		for (int index = 0; index < children.getLength(); index++) {
			collectElements(children.item(index), elements);
		}
	}

	private void inferTypes(Collection<Element> elements, PlainXmlMetaClass elementClass) {
		Map<String, PlainXmlMetaClass> classesByTag = new LinkedHashMap<>();
		for (Element element : elements) {
			String tagName = getTypeTagName(element);
			if (!classesByTag.containsKey(tagName)) {
				PlainXmlMetaClass metaClass = new PlainXmlMetaClass("t_" + tagName, this);
				metaClass.getSuperTypes().add(elementClass);
				elementClass.getSubTypes().add(metaClass);
				classesByTag.put(tagName, metaClass);
				getTypes().add(metaClass);
			}
		}

		for (Element element : elements) {
			PlainXmlMetaClass metaClass = classesByTag.get(getTypeTagName(element));
			addAttributeProperties(metaClass, element.getAttributes());
			for (Element child : DomUtil.getChildren(element)) {
				String childTagName = getTypeTagName(child);
				PlainXmlMetaClass childClass = classesByTag.get(childTagName);
				EolModelElementType childType = new EolModelElementType(childClass);
				addProperty(metaClass, "e_" + childTagName, childType);
				addProperty(metaClass, "c_" + childTagName, new EolCollectionType("Sequence", childType));
			}
		}
	}

	private String getTypeTagName(Element element) {
		String tagName = element.getTagName();
		int colonIndex = tagName.indexOf(':');
		return (colonIndex >= 0 ? tagName.substring(colonIndex + 1) : tagName).toLowerCase(Locale.ROOT);
	}

	private void addCoreProperties(PlainXmlMetaClass elementClass) {
		EolModelElementType elementType = new EolModelElementType(elementClass);
		EolCollectionType elementsType = new EolCollectionType("Sequence", elementType);
		addProperty(elementClass, "name", EolPrimitiveType.String);
		addProperty(elementClass, "tagName", EolPrimitiveType.String);
		addProperty(elementClass, "text", EolPrimitiveType.String);
		addProperty(elementClass, "parent", elementType);
		addProperty(elementClass, "parentNode", new EolNativeType(Node.class));
		addProperty(elementClass, "children", elementsType);
		addProperty(elementClass, "descendants", new EolCollectionType("Sequence", elementType));
	}

	private void addAttributeProperties(PlainXmlMetaClass metaClass, NamedNodeMap attributes) {
		for (int index = 0; index < attributes.getLength(); index++) {
			String name = attributes.item(index).getNodeName();
			addProperty(metaClass, "a_" + name, EolPrimitiveType.String);
			addProperty(metaClass, "s_" + name, EolPrimitiveType.String);
			addProperty(metaClass, "b_" + name, EolPrimitiveType.Boolean);
			addProperty(metaClass, "i_" + name, EolPrimitiveType.Integer);
			addProperty(metaClass, "f_" + name, EolPrimitiveType.Real);
			addProperty(metaClass, "d_" + name, EolPrimitiveType.Real);
			addProperty(metaClass, "r_" + name, EolPrimitiveType.Real);
		}
	}

	private void addProperty(PlainXmlMetaClass metaClass, String name, EolType type) {
		if (metaClass.getProperty(name) == null) {
			metaClass.getProperties().add(new PlainXmlMetaProperty(name, type));
		}
	}

	private static String getSource(String example, IRelativePathResolver resolver, String modelName) {
		if (!StringUtil.isEmpty(example)) {
			String resolved = resolver != null ? resolver.resolve(example) : example;
			try {
				return new File(resolved).getCanonicalFile().toURI().toString();
			}
			catch (IOException ex) {
				return new File(resolved).getAbsoluteFile().toURI().toString();
			}
		}
		return "model:" + modelName;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof PlainXmlModelMetamodel)) return false;
		return Objects.equals(source, ((PlainXmlModelMetamodel) other).source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, getClass());
	}

	private static class PlainXmlMetaProperty implements IProperty {
		private final String name;
		private final EolType type;

		PlainXmlMetaProperty(String name, EolType type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public EolType getType() {
			return type;
		}
	}
}
