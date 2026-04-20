package org.eclipse.epsilon.egx.staticanalyser;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.egl.EgxModule;
import org.eclipse.epsilon.egl.dom.GenerationRule;
import org.eclipse.epsilon.egl.dom.IEgxVisitor;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.ReturnStatement;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.eol.staticanalyser.IModelFactory;
import org.eclipse.epsilon.eol.staticanalyser.execute.context.Variable;
import org.eclipse.epsilon.eol.staticanalyser.types.EolNativeType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;
import org.eclipse.epsilon.erl.dom.Post;
import org.eclipse.epsilon.erl.dom.Pre;

public class EgxStaticAnalyser extends EolStaticAnalyser implements IEgxVisitor {

	public EgxStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}

	public EgxStaticAnalyser() {
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof EgxModule))
			return Collections.emptyList();

		this.module = (EgxModule) imodule;
		EgxModule egxModule = (EgxModule) imodule;

		super.preValidate(egxModule);

		for (Pre pre : egxModule.getPre()) {
			pre.accept(this);
		}

		super.mainValidate();

		for (GenerationRule rule : egxModule.getDeclaredGenerationRules()) {
			rule.accept(this);
		}

		for (Post post : egxModule.getPost()) {
			post.accept(this);
		}

		super.postValidate();

		return markers;
	}

	@Override
	public void visit(Pre pre) {
		pre.getBody().accept(this);
	}

	@Override
	public void visit(Post post) {
		post.getBody().accept(this);
	}

	@Override
	public void visit(GenerationRule rule) {
		context.getFrameStack().enterLocal(FrameType.UNPROTECTED, rule);

		// Resolve the source parameter type (e.g. "t : Tree" binds 't' to type Tree)
		if (rule.getTransformSource() != null) {
			rule.getTransformSource().accept(this);
		}

		// Visit domain block
		if (rule.getDomainBlock() != null) {
			rule.getDomainBlock().accept(this);
		}

		// Check guard is Boolean
		checkBoolean(rule.getGuardBlock());

		// Visit pre block
		if (rule.getPreBlock() != null) {
			rule.getPreBlock().accept(this);
		}

		// Visit template block
		if (rule.getTemplateBlock() != null) {
			rule.getTemplateBlock().accept(this);
		}

		// Visit target block
		if (rule.getTargetBlock() != null) {
			rule.getTargetBlock().accept(this);
		}

		// Visit parameters block
		if (rule.getParametersBlock() != null) {
			rule.getParametersBlock().accept(this);
		}

		// Check Boolean blocks
		checkBoolean(rule.getOverwriteBlock());
		checkBoolean(rule.getMergeBlock());

		// Visit post block with 'generated' variable (mirrors GenerationRule.java runtime behaviour)
		// Type is File when a target is specified, String otherwise
		if (rule.getPostBlock() != null) {
			EolType generatedType = rule.getTargetBlock() != null
				? new EolNativeType(File.class)
				: EolPrimitiveType.String;
			context.getFrameStack().enterLocal(FrameType.UNPROTECTED, rule.getPostBlock(),
				new Variable("generated", generatedType));
			rule.getPostBlock().accept(this);
			context.getFrameStack().leaveLocal(rule.getPostBlock());
		}

		// Visit formatter block
		if (rule.getFormatterBlock() != null) {
			rule.getFormatterBlock().accept(this);
		}

		context.getFrameStack().leaveLocal(rule);
	}

	private void checkBoolean(ExecutableBlock<Boolean> block) {
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

	@Override
	public EolType expectedReturnType(ReturnStatement returnStatement) {
		ModuleElement parent = returnStatement.getParent();
		while (!(parent instanceof ExecutableBlock) && parent != null) {
			parent = parent.getParent();
		}
		if (parent instanceof ExecutableBlock) {
			return javaClassToEolType(((ExecutableBlock<?>) parent).getExpectedResultClass());
		} else {
			return super.expectedReturnType(returnStatement);
		}
	}
}
