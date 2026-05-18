package org.eclipse.epsilon.eol.staticanalyser;

import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class EolCompletion {

	protected final String name;
	protected final EolCompletionKind kind;
	protected final EolType type;
	protected final String detail;
	protected final String label;

	public EolCompletion(String name, EolCompletionKind kind, EolType type) {
		this(name, kind, type, type != null ? type.toString() : "Any");
	}

	public EolCompletion(String name, EolCompletionKind kind, EolType type, String detail) {
		this(name, kind, type, detail, name);
	}

	public EolCompletion(String name, EolCompletionKind kind, EolType type, String detail, String label) {
		this.name = name;
		this.kind = kind;
		this.type = type;
		this.detail = detail != null && !detail.isEmpty() ? detail : (type != null ? type.toString() : "Any");
		this.label = label != null && !label.isEmpty() ? label : name;
	}

	public String getName() {
		return name;
	}

	public EolCompletionKind getKind() {
		return kind;
	}

	public EolType getType() {
		return type;
	}

	public String getDetail() {
		return detail;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public String toString() {
		return kind + " " + label + " : " + getDetail();
	}
}
