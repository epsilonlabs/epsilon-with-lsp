package org.eclipse.epsilon.erl.analyse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.analyse.EolStaticAnalyser;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.types.EolPrimitiveType;
import org.eclipse.epsilon.erl.IErlModule;
import org.eclipse.epsilon.erl.dom.IErlVisitor;
import org.eclipse.epsilon.erl.dom.Post;
import org.eclipse.epsilon.erl.dom.Pre;

/**
 * Static analyser for the parts that all ERL-based languages share: operations and
 * pre/post blocks. Subclasses analyse their own rules in {@link #validateRules(IErlModule)}.
 */
public class ErlStaticAnalyser extends EolStaticAnalyser implements IErlVisitor {

	public ErlStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public ErlStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof IErlModule))
			return Collections.emptyList();

		IErlModule erlModule = (IErlModule) imodule;
		markers = new ArrayList<>();
		visibleVariablesRegistry = new ArrayList<>();
		this.module = erlModule;

		preValidate(erlModule);
		registerBuiltins(erlModule);

		for (Pre pre : erlModule.getPre()) {
			if (isDeclared(pre, erlModule.getDeclaredPre())) {
				pre.accept(this);
			}
			else {
				visitImported(() -> pre.accept(this));
			}
		}

		mainValidate();
		validateRules(erlModule);

		for (Post post : erlModule.getPost()) {
			if (isDeclared(post, erlModule.getDeclaredPost())) {
				post.accept(this);
			}
			else {
				visitImported(() -> post.accept(this));
			}
		}

		postValidate();

		return markers;
	}

	/**
	 * Registers the built-in operations and variables of the language. Called once per
	 * validation, after {@link #preValidate} and before any block is visited.
	 * Unlike {@link #preValidate}, it is not called again for imported modules.
	 */
	protected void registerBuiltins(IErlModule module) {
	}

	/**
	 * Visits the rules declared in the module, after its operations and before its post blocks.
	 */
	protected void validateRules(IErlModule module) {
	}

	@Override
	public void visit(Pre pre) {
		pre.getBody().accept(this);
	}

	@Override
	public void visit(Post post) {
		post.getBody().accept(this);
	}

	/**
	 * Visits a block that should evaluate to a Boolean, such as a guard, and reports an
	 * error if its body is an expression of another type.
	 */
	protected void checkBoolean(ExecutableBlock<Boolean> block) {
		if (block != null) {
			block.accept(this);
			if (block.getBody() instanceof Expression) {
				if (!getResolvedType((Expression) block.getBody()).equals(EolPrimitiveType.Boolean)) {
					markers.add(new ModuleMarker(block, "Expression type should be Boolean instead of "
							+ getResolvedType((Expression) block.getBody()), Severity.Error));
				}
			}
		}
	}

	/**
	 * Visits a block of an imported module so that the variables it declares are in scope,
	 * but discards the markers and visible variables recorded meanwhile: they belong to the
	 * imported file, and are reported when that file is analysed.
	 */
	private void visitImported(Runnable visit) {
		int markerCount = markers.size();
		int snapshotCount = visibleVariablesRegistry.size();
		visit.run();
		markers.subList(markerCount, markers.size()).clear();
		visibleVariablesRegistry.subList(snapshotCount, visibleVariablesRegistry.size()).clear();
	}

	private static boolean isDeclared(Object block, List<?> declaredBlocks) {
		for (Object declaredBlock : declaredBlocks) {
			if (declaredBlock == block) {
				return true;
			}
		}
		return false;
	}
}
