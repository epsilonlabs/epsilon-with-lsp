/*******************************************************************************
 * Copyright (c) 2012 The University of York.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors:
 *     Dimitrios Kolovos - initial API and implementation
 ******************************************************************************/
package org.eclipse.epsilon.emc.plainxml;

public class Binding {

	private static final String DECLARATION_FORMAT =
		"sourceTag.sourceAttribute:targetTag.targetAttribute:boolean";
	
	protected String sourceTag;
	protected String sourceAttribute;
	protected String targetTag;
	protected String targetAttribute;
	protected boolean many;
	
	public Binding(String sourceTag, String sourceAttribute, String targetTag, String targetAttribute, boolean many) {
		super();
		
		if ("*".equals(sourceTag)) sourceTag =  ".*";
		if ("*".equals(targetTag)) targetTag =  ".*";
		
		this.sourceTag = sourceTag;
		this.sourceAttribute = sourceAttribute;
		this.targetTag = targetTag;
		this.targetAttribute = targetAttribute;
		this.many = many;
	}

	public static Binding parse(String declaration) {
		String[] parts = declaration == null ? new String[0] : declaration.trim().split(":", -1);
		if (parts.length != 3) {
			throw new IllegalArgumentException("expected " + DECLARATION_FORMAT);
		}

		String[] source = parseEndpoint(parts[0]);
		String[] target = parseEndpoint(parts[1]);
		String many = parts[2].trim();
		if (!"true".equalsIgnoreCase(many) && !"false".equalsIgnoreCase(many)) {
			throw new IllegalArgumentException("expected true or false for multiplicity");
		}

		return new Binding(source[0], source[1], target[0], target[1], Boolean.parseBoolean(many));
	}

	private static String[] parseEndpoint(String endpoint) {
		String trimmedEndpoint = endpoint.trim();
		int separator = trimmedEndpoint.lastIndexOf('.');
		if (separator <= 0 || separator == trimmedEndpoint.length() - 1) {
			throw new IllegalArgumentException("expected " + DECLARATION_FORMAT);
		}
		return new String[] {
			trimmedEndpoint.substring(0, separator).trim(), trimmedEndpoint.substring(separator + 1).trim()
		};
	}

	public String getSourceTag() {
		return sourceTag;
	}
		
	public String getSourceAttribute() {
		return sourceAttribute;
	}
		
	public String getTargetTag() {
		return targetTag;
	}
	
	public String getTargetAttribute() {
		return targetAttribute;
	}
	
	public boolean isMany() {
		return many;
	}
}
