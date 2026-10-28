package org.eclipse.epsilon.eol.analyse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.eol.types.EolType;

public class EolCompletion {

	protected final String name;
	protected final EolCompletionKind kind;
	protected final EolType type;
	protected final String detail;
	protected final String label;
	protected final List<String> parameterNames;
	protected final boolean expectedTypeMatch;

	public EolCompletion(String name, EolCompletionKind kind, EolType type) {
		this(name, kind, type, type != null ? type.toString() : "Any");
	}

	public EolCompletion(String name, EolCompletionKind kind, EolType type, String detail) {
		this(name, kind, type, detail, name);
	}

	public EolCompletion(String name, EolCompletionKind kind, EolType type, String detail, String label) {
		this(name, kind, type, detail, label, Collections.emptyList());
	}

	public EolCompletion(String name, EolCompletionKind kind, EolType type, String detail, String label,
			List<String> parameterNames) {
		this(name, kind, type, detail, label, parameterNames, false);
	}

	public EolCompletion(String name, EolCompletionKind kind, EolType type, String detail, String label,
			List<String> parameterNames, boolean expectedTypeMatch) {
		this.name = name;
		this.kind = kind;
		this.type = type;
		this.detail = detail != null && !detail.isEmpty() ? detail : (type != null ? type.toString() : "Any");
		this.label = label != null && !label.isEmpty() ? label : name;
		this.parameterNames = Collections.unmodifiableList(new ArrayList<String>(parameterNames));
		this.expectedTypeMatch = expectedTypeMatch;
	}

	public EolCompletion withExpectedTypeMatch(boolean expectedTypeMatch) {
		return new EolCompletion(name, kind, type, detail, label, parameterNames, expectedTypeMatch);
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

	public List<String> getParameterNames() {
		return parameterNames;
	}

	/**
	 * Whether the value of this completion is assignable to the type expected at
	 * the completion position (e.g. the type of the enclosing operation call's
	 * parameter). Clients rank such completions first.
	 */
	public boolean isExpectedTypeMatch() {
		return expectedTypeMatch;
	}

	@Override
	public String toString() {
		return kind + " " + label + " : " + getDetail();
	}
}
