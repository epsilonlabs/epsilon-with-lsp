package org.eclipse.epsilon.eol.staticanalyser;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.IModuleValidator;
import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.common.util.StringUtil;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.dom.AbortStatement;
import org.eclipse.epsilon.eol.dom.AndOperatorExpression;
import org.eclipse.epsilon.eol.dom.AnnotationBlock;
import org.eclipse.epsilon.eol.dom.AssignmentStatement;
import org.eclipse.epsilon.eol.dom.BooleanLiteral;
import org.eclipse.epsilon.eol.dom.BreakStatement;
import org.eclipse.epsilon.eol.dom.Case;
import org.eclipse.epsilon.eol.dom.CollectionLiteralExpression;
import org.eclipse.epsilon.eol.dom.ComplexOperationCallExpression;
import org.eclipse.epsilon.eol.dom.ContinueStatement;
import org.eclipse.epsilon.eol.dom.DeleteStatement;
import org.eclipse.epsilon.eol.dom.DivOperatorExpression;
import org.eclipse.epsilon.eol.dom.DoubleEqualsOperatorExpression;
import org.eclipse.epsilon.eol.dom.ElvisOperatorExpression;
import org.eclipse.epsilon.eol.dom.EnumerationLiteralExpression;
import org.eclipse.epsilon.eol.dom.EqualsOperatorExpression;
import org.eclipse.epsilon.eol.dom.ExecutableAnnotation;
import org.eclipse.epsilon.eol.dom.ExecutableBlock;
import org.eclipse.epsilon.eol.dom.Expression;
import org.eclipse.epsilon.eol.dom.ExpressionInBrackets;
import org.eclipse.epsilon.eol.dom.ExpressionStatement;
import org.eclipse.epsilon.eol.dom.FirstOrderOperationCallExpression;
import org.eclipse.epsilon.eol.dom.ForStatement;
import org.eclipse.epsilon.eol.dom.GreaterEqualOperatorExpression;
import org.eclipse.epsilon.eol.dom.GreaterThanOperatorExpression;
import org.eclipse.epsilon.eol.dom.IEolVisitor;
import org.eclipse.epsilon.eol.dom.IfStatement;
import org.eclipse.epsilon.eol.dom.ImpliesOperatorExpression;
import org.eclipse.epsilon.eol.dom.Import;
import org.eclipse.epsilon.eol.dom.IntegerLiteral;
import org.eclipse.epsilon.eol.dom.ItemSelectorExpression;
import org.eclipse.epsilon.eol.dom.LessEqualOperatorExpression;
import org.eclipse.epsilon.eol.dom.LessThanOperatorExpression;
import org.eclipse.epsilon.eol.dom.MapLiteralExpression;
import org.eclipse.epsilon.eol.dom.MinusOperatorExpression;
import org.eclipse.epsilon.eol.dom.ModelDeclaration;
import org.eclipse.epsilon.eol.dom.ModelDeclarationParameter;
import org.eclipse.epsilon.eol.dom.NameExpression;
import org.eclipse.epsilon.eol.dom.NegativeOperatorExpression;
import org.eclipse.epsilon.eol.dom.NewInstanceExpression;
import org.eclipse.epsilon.eol.dom.NotEqualsOperatorExpression;
import org.eclipse.epsilon.eol.dom.NotOperatorExpression;
import org.eclipse.epsilon.eol.dom.Operation;
import org.eclipse.epsilon.eol.dom.OperationCallExpression;
import org.eclipse.epsilon.eol.dom.OperatorExpression;
import org.eclipse.epsilon.eol.dom.OrOperatorExpression;
import org.eclipse.epsilon.eol.dom.Parameter;
import org.eclipse.epsilon.eol.dom.PlusOperatorExpression;
import org.eclipse.epsilon.eol.dom.PostfixOperatorExpression;
import org.eclipse.epsilon.eol.dom.PropertyCallExpression;
import org.eclipse.epsilon.eol.dom.RealLiteral;
import org.eclipse.epsilon.eol.dom.ReturnStatement;
import org.eclipse.epsilon.eol.dom.SimpleAnnotation;
import org.eclipse.epsilon.eol.dom.StatementBlock;
import org.eclipse.epsilon.eol.dom.StringLiteral;
import org.eclipse.epsilon.eol.dom.SwitchStatement;
import org.eclipse.epsilon.eol.dom.TernaryExpression;
import org.eclipse.epsilon.eol.dom.ThrowStatement;
import org.eclipse.epsilon.eol.dom.TimesOperatorExpression;
import org.eclipse.epsilon.eol.dom.TransactionStatement;
import org.eclipse.epsilon.eol.dom.TypeExpression;
import org.eclipse.epsilon.eol.dom.VariableDeclaration;
import org.eclipse.epsilon.eol.dom.WhileStatement;
import org.eclipse.epsilon.eol.dom.XorOperatorExpression;
import org.eclipse.epsilon.eol.staticanalyser.execute.context.FrameStack;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.operations.AbstractOperation;
import org.eclipse.epsilon.eol.execute.operations.TypeCalculator;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributor;
import org.eclipse.epsilon.eol.staticanalyser.execute.context.Variable;
import org.eclipse.epsilon.eol.m3.MetaClass;
import org.eclipse.epsilon.eol.m3.Metamodel;
import org.eclipse.epsilon.eol.m3.StructuralFeature;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.ModelGroup;
import org.eclipse.epsilon.eol.models.ModelRepository.TypeAmbiguityCheckResult;
import org.eclipse.epsilon.eol.staticanalyser.types.EolSelf;
import org.eclipse.epsilon.eol.staticanalyser.types.EolSelfCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolSelfContentType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolSelfExpressionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolUnionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolTupleType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolAnyType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolCollectionType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolMapType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolModelElementType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolNativeType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolNoType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolPrimitiveType;
import org.eclipse.epsilon.eol.staticanalyser.types.EolType;

public class EolStaticAnalyser implements IModuleValidator, IEolVisitor {

	protected List<ModuleMarker> errors = new ArrayList<>();
	protected List<ModuleMarker> warnings = new ArrayList<>();
	protected EolModule module;
	protected EolStaticAnalysisContext context = new EolStaticAnalysisContext();
	protected List<IStaticOperation> operations = new ArrayList<>();
	HashMap<Operation, Boolean> returnFlags = new HashMap<>(); // for every missmatch

	public EolStaticAnalyser() {
	}

	public EolStaticAnalyser(IModelFactory modelFactory) {
		context.modelFactory = modelFactory;
	}

	@Override
	public void visit(AbortStatement abortStatement) {
	}

	@Override
	public void visit(AndOperatorExpression andOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) andOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(DeleteStatement deleteStatement) {
		deleteStatement.getExpression().accept(this);
	}

	@Override
	public void visit(AnnotationBlock annotationBlock) {
	}

	@Override
	public void visit(AssignmentStatement assignmentStatement) {

		Expression targetExpression = assignmentStatement.getTargetExpression();
		Expression valueExpression = assignmentStatement.getValueExpression();

		valueExpression.accept(this);
		targetExpression.accept(this);

		EolType targetType = getResolvedType(targetExpression);
		EolType valueType = getResolvedType(valueExpression);

		if (targetType instanceof EolModelElementType && ((EolModelElementType) targetType).getMetaClass() != null)
			targetType = new EolModelElementType(((EolModelElementType) targetType).getMetaClass());
		if (valueType instanceof EolModelElementType && ((EolModelElementType) valueType).getMetaClass() != null)
			valueType = new EolModelElementType(((EolModelElementType) valueType).getMetaClass());

		if (!targetType.isAncestorOf(valueType)) {
			if (canBeCompatible(targetType, valueType)) {
				createTypeCompatibilityWarning(targetExpression, valueExpression);
			} else {
				createTypeCompatibilityError(targetExpression, valueExpression);
			}
		}
	}

	@Override
	public void visit(BooleanLiteral booleanLiteral) {
		setResolvedType(booleanLiteral, EolPrimitiveType.Boolean);
	}

	@Override
	public void visit(BreakStatement breakStatement) {
	}

	@Override
	public void visit(Case case_) {
	}

	@Override
	public void visit(CollectionLiteralExpression<?> collectionLiteralExpression) {
		if (!collectionLiteralExpression.getParameterExpressions().isEmpty()) {
			collectionLiteralExpression.getParameterExpressions().get(0).accept(this);
			setResolvedType(collectionLiteralExpression,
					new EolCollectionType(collectionLiteralExpression.getCollectionType(),
							getResolvedType(collectionLiteralExpression.getParameterExpressions().get(0))));
		}
	}

	@Override
	public void visit(ComplexOperationCallExpression complexOperationCallExpression) {
	}

	@Override
	public void visit(ContinueStatement continueStatement) {
	}

	@Override
	public void visit(DivOperatorExpression divOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) divOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(DoubleEqualsOperatorExpression doubleEqualsOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) doubleEqualsOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(ElvisOperatorExpression elvisOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) elvisOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(EnumerationLiteralExpression enumerationLiteralExpression) {
	}

	@Override
	public void visit(EqualsOperatorExpression equalsOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) equalsOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(ExecutableAnnotation executableAnnotation) {
		executableAnnotation.getExpression().accept(this);
	}

	@Override
	public void visit(ExecutableBlock<?> executableBlock) {
		Object body = executableBlock.getBody();
		if (body instanceof StatementBlock) {
			((StatementBlock) body).accept(this);
		} else if (body instanceof Expression) {
			((Expression) body).accept(this);
		}
		// Should we add add accept method?
	}

	@Override
	public void visit(ExpressionInBrackets expressionInBrackets) {
		expressionInBrackets.getExpression().accept(this);
		setResolvedType(expressionInBrackets, getResolvedType(expressionInBrackets.getExpression()));
	}

	@Override
	public void visit(ExpressionStatement expressionStatement) {
		expressionStatement.getExpression().accept(this);

	}

	@Override
	public void visit(FirstOrderOperationCallExpression firstOrderOperationCallExpression) {
		String name = firstOrderOperationCallExpression.getName();
		AbstractOperation operation = context.operationFactory.getOperationFor(name);
		if (operation == null) {
			errors.add(new ModuleMarker(firstOrderOperationCallExpression.getNameExpression(),
					"Undefined first order operation " + name, Severity.Error));
			return;
		}
		TypeCalculator tc = operation.getClass().getAnnotation(TypeCalculator.class);
		if (tc == null) {
			setResolvedType(firstOrderOperationCallExpression, EolAnyType.Instance);
			return;
		}

		Expression targetExpression = firstOrderOperationCallExpression.getTargetExpression();
		targetExpression.accept(this);
		EolType contextType = getResolvedType(targetExpression);
		if (!(contextType instanceof EolCollectionType)) {
			contextType = new EolCollectionType("Sequence", contextType);
		}

		Parameter iterator = firstOrderOperationCallExpression.getParameters().get(0);
		visit(iterator, false);
		EolType iteratorType = getType(iterator);
		if (iteratorType.equals(EolAnyType.Instance)) {
			iteratorType = ((EolCollectionType) contextType).getContentType();
		}

		context.getFrameStack().enterLocal(FrameType.UNPROTECTED, firstOrderOperationCallExpression,
				new Variable(iterator.getName(), iteratorType));
		Expression expression = firstOrderOperationCallExpression.getExpressions().get(0);
		expression.accept(this);
		EolType expressionType = getResolvedType(expression);
		context.getFrameStack().leaveLocal(firstOrderOperationCallExpression);

		try {
			EolType returnType = tc.klass().newInstance().calculateType(contextType, iteratorType, expressionType);
			setResolvedType(firstOrderOperationCallExpression, returnType);
		} catch (Exception e) {
			setResolvedType(firstOrderOperationCallExpression, EolAnyType.Instance);
			e.printStackTrace();
		}
	}

	@Override
	public void visit(ForStatement forStatement) {

		forStatement.getIteratedExpression().accept(this);
		context.getFrameStack().enterLocal(FrameType.UNPROTECTED, forStatement.getBodyStatementBlock(),
				new Variable("loopCount", EolPrimitiveType.Integer), new Variable("hasMore", EolPrimitiveType.Boolean));

		Parameter iteratorParameter = forStatement.getIteratorParameter();
		iteratorParameter.accept(this);
		if (getType(iteratorParameter) == EolAnyType.Instance) {
			EolType iteratorType = getResolvedType(forStatement.getIteratedExpression());
			if (iteratorType instanceof EolCollectionType) {
				context.getFrameStack().get(iteratorParameter.getName())
						.setType(((EolCollectionType) iteratorType).getContentType());
			}
		}
		forStatement.getBodyStatementBlock().accept(this);
		context.getFrameStack().leaveLocal(forStatement.getBodyStatementBlock());

		if (hasResolvedType(forStatement.getIteratedExpression())
				&& !(getResolvedType(forStatement.getIteratedExpression()) instanceof EolCollectionType)) {
			errors.add(new ModuleMarker(forStatement.getIteratedExpression(),
					"Collection expected instead of " + getResolvedType(forStatement.getIteratedExpression()),
					Severity.Error));
		}
	}

	@Override
	public void visit(GreaterEqualOperatorExpression greaterEqualOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) greaterEqualOperatorExpression;
		visitOperatorExpression(operatorExpression);

	}

	@Override
	public void visit(GreaterThanOperatorExpression greaterThanOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) greaterThanOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(IfStatement ifStatement) {

		Expression conditionExpression = ifStatement.getConditionExpression();
		StatementBlock thenStatementBlock = ifStatement.getThenStatementBlock();
		StatementBlock elseStatementBlock = ifStatement.getElseStatementBlock();

		conditionExpression.accept(this);
		FrameStack frameStack = context.getFrameStack();
		frameStack.enterLocal(FrameType.UNPROTECTED, thenStatementBlock);
		thenStatementBlock.accept(this);
		frameStack.leaveLocal(thenStatementBlock);

		if (elseStatementBlock != null) {
			frameStack.enterLocal(FrameType.UNPROTECTED, elseStatementBlock);
			elseStatementBlock.accept(this);
			context.getFrameStack().leaveLocal(elseStatementBlock);
		}

		if (hasResolvedType(conditionExpression) && getResolvedType(conditionExpression) != EolPrimitiveType.Boolean) {
			errors.add(new ModuleMarker(conditionExpression, "Condition must be a Boolean", Severity.Error));
		}

	}

	@Override
	public void visit(ImpliesOperatorExpression impliesOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) impliesOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(Import import_) {
	}

	@Override
	public void visit(IntegerLiteral integerLiteral) {
		setResolvedType(integerLiteral, EolPrimitiveType.Integer);

	}

	@Override
	public void visit(ItemSelectorExpression itemSelectorExpression) {

		itemSelectorExpression.getTargetExpression().accept(this);
		itemSelectorExpression.getIndexExpression().accept(this);

		EolType targetExpressionType = getResolvedType(itemSelectorExpression.getTargetExpression());
		if (targetExpressionType != EolAnyType.Instance) {
			if (targetExpressionType instanceof EolCollectionType) {
				setResolvedType(itemSelectorExpression, ((EolCollectionType) targetExpressionType).getContentType());
			} else {
				errors.add(new ModuleMarker(itemSelectorExpression.getIndexExpression(),
						"[...] only applies to collections", Severity.Error));
			}
		}

	}

	@Override
	public void visit(LessEqualOperatorExpression lessEqualOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) lessEqualOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(LessThanOperatorExpression lessThanOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) lessThanOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(MapLiteralExpression<?, ?> mapLiteralExpression) {
	}

	@Override
	public void visit(MinusOperatorExpression minusOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) minusOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(ModelDeclaration modelDeclaration) {

		if (context.getModelFactory() == null)
			return;

		String modelName = modelDeclaration.getNameExpression().getName();
		IModel model = context.getModelFactory().createModel(modelDeclaration.getDriverNameExpression().getName());
		model.setName(modelName);

		for (NameExpression alias : modelDeclaration.getAliasNameExpressions()) {
			model.getAliases().add(alias.getName());
		}

		context.getRepository().addModel(model);
		modelDeclaration.setModel(model);
		context.getModelDeclarations().put(modelName, modelDeclaration);

//		if (context.getModels().containsKey(modelName)) {
//			errors.add(new ModuleMarker(modelDeclaration, "Duplicate model name", Severity.Error));
//		}
//		else {
//			context.getModels().put(modelDeclaration.getNameExpression().getName(), modelDeclaration.getModel());
//		}

//		for (NameExpression alias : modelDeclaration.getAliasNameExpressions()) {
//			String aliasName = alias.getName();
//			if (context.getModels().containsKey(aliasName)){
//				if (context.getModels().get(aliasName) instanceof ModelGroup) {
//					((ModelGroup)context.getModels().get(aliasName)).getModels().add(model);
//				}
//				else {
//					errors.add(new ModuleMarker(modelDeclaration, "An alias cannot overlap with a model name", Severity.Error));
//				}
//			}
//			else {
//				context.getModels().put(aliasName, new ModelGroup());
//			}
//		}

		if (modelDeclaration.getModel() == null) {
			context.addErrorMarker(modelDeclaration.getDriverNameExpression(),
					"Unknown type of model: " + modelDeclaration.getDriverNameExpression().getName());
		} else {
			StringProperties stringProperties = new StringProperties();
			for (ModelDeclarationParameter parameter : modelDeclaration.getModelDeclarationParameters()) {
				stringProperties.put(parameter.getKey(), parameter.getValue());
			}
			modelDeclaration.setMetamodel(
					modelDeclaration.getModel().getMetamodel(stringProperties, context.getRelativePathResolver()));
			if (modelDeclaration.getMetamodel() != null) {
				for (String error : modelDeclaration.getMetamodel().getErrors()) {
					errors.add(new ModuleMarker(modelDeclaration, error, Severity.Error));
				}
				for (String warning : modelDeclaration.getMetamodel().getWarnings()) {
					warnings.add(new ModuleMarker(modelDeclaration, warning, Severity.Warning));
				}
			}
		}

	}

	@Override
	public void visit(ModelDeclarationParameter modelDeclarationParameter) {
	}

	@Override
	public void visit(NameExpression nameExpression) {

		EolModelElementType modelElementType;
		Variable variable = context.getFrameStack().get(nameExpression.getName());
		if (variable != null) {
			setResolvedType(nameExpression, variable.getType());
		} else {
			modelElementType = getModelElementType(nameExpression.getName(), nameExpression);
			if (modelElementType != null) {
				setResolvedType(nameExpression, modelElementType);
				nameExpression.setTypeName(true);
				if (modelElementType.getMetaClass() == null && !context.getModelDeclarations().isEmpty()) {

					errors.add(new ModuleMarker(nameExpression, "Unknown type " + nameExpression.getName(),
							Severity.Error));
				}

			} else {

				errors.add(new ModuleMarker(nameExpression, "Undefined variable or type " + nameExpression.getName(),
						Severity.Error));
			}
		}
	}

	@Override
	public void visit(NegativeOperatorExpression negativeOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) negativeOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(NewInstanceExpression newInstanceExpression) {

		newInstanceExpression.getTypeExpression().accept(this);
		for (Expression parameterExpression : newInstanceExpression.getParameterExpressions()) {
			parameterExpression.accept(this);
		}
		setResolvedType(newInstanceExpression, getResolvedType(newInstanceExpression.getTypeExpression()));
	}

	@Override
	public void visit(NotEqualsOperatorExpression notEqualsOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) notEqualsOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(NotOperatorExpression notOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) notOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(Operation operation) {
		EolType contextType = (EolType) operation.getData().get("contextType");
		TypeExpression returnTypeExpression = operation.getReturnTypeExpression();
		setReturnFlag(operation, false);

		// Variable class changed
		context.getFrameStack().enterLocal(FrameType.PROTECTED, operation, new Variable("self", contextType));

		for (Parameter parameter : operation.getFormalParameters()) {
			parameter.accept(this);
		}
		operation.getBody().accept(this);

		if (getReturnFlag(operation) == false && returnTypeExpression != null)
			errors.add(new ModuleMarker(returnTypeExpression,
					"This operation should return " + returnTypeExpression.getName(), Severity.Error));
		context.getFrameStack().leaveLocal(operation);

	}

	@Override
	public void visit(OperationCallExpression operationCallExpression) {
		List<IStaticOperation> resolvedOperations = new ArrayList<>(operations);
		Expression targetExpression = operationCallExpression.getTargetExpression();
		List<Expression> parameterExpressions = operationCallExpression.getParameterExpressions();
		NameExpression nameExpression = operationCallExpression.getNameExpression();
		EolType contextType = EolNoType.Instance;

		if (targetExpression != null) {
			targetExpression.accept(this);
			operationCallExpression.setContextless(false);
			contextType = getResolvedType(targetExpression);
		} else
			operationCallExpression.setContextless(true);
		for (Expression parameterExpression : parameterExpressions) {
			parameterExpression.accept(this);
		}

		// Name check
		List<IStaticOperation> temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			if (nameExpression.getName().equals(op.getName())) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			errors.add(new ModuleMarker(nameExpression, "Undefined operation", Severity.Error));
			return;
		}

		// Context check
		temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			EolType opContextType = op.getContextType();
			if (contextType == opContextType || opContextType.isAncestorOf(contextType)
					|| canBeCompatible(opContextType, contextType)) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			errors.add(new ModuleMarker(targetExpression,
					nameExpression.getName() + " can not be invoked on " + getResolvedType(targetExpression),
					Severity.Error));
			return;
		}

//		Number of parameters check
		temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			List<EolType> reqParams = op.getParameterTypes();
			if (reqParams.size() == parameterExpressions.size()) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			errors.add(new ModuleMarker(nameExpression, "Parameter number mismatch", Severity.Error));
			return;
		}

//		Parameter type checks
		temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			int index = 0;
			List<EolType> reqParamTypess = op.getParameterTypes();
			boolean compatible = true;
			for (EolType reqParamType : reqParamTypess) {
				EolType provParamType = getResolvedType(parameterExpressions.get(index));
				index++;
				if (!reqParamType.isAncestorOf(provParamType) && !canBeCompatible(reqParamType, provParamType)) {
					compatible = false;
					break;
				}
			}
			if (compatible) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			errors.add(new ModuleMarker(nameExpression, "Parameters type mismatch", Severity.Error));
			return;
		}

		// Process resolved operations
		Set<EolType> returnTypes = resolvedOperations.stream().map(op -> op.getReturnType())
				.collect(Collectors.toSet());
		if (returnTypes.size() == 1) {
			setResolvedType(operationCallExpression, (EolType) returnTypes.toArray()[0]);
		} else {
			setResolvedType(operationCallExpression, new EolUnionType(returnTypes));
		}

		// Check for warning related to subtypes
		Set<EolType> resolvedOperationContextTypes = new HashSet<EolType>();
		for (IStaticOperation op : resolvedOperations) {
			resolvedOperationContextTypes.add(op.getContextType());
		}

		Stack<EolType> stack = new Stack<EolType>();
		stack.push(contextType);
		outerLoop: while (!stack.isEmpty()) {
			EolType currentNode = stack.pop();
			for (EolType a : currentNode.getAncestors()) {
				if (resolvedOperationContextTypes.contains(a)) {
					continue outerLoop;
				}
			}
			if (currentNode.isAbstract()) {
				stack.addAll(currentNode.getChildrenTypes());
			} else {
				warnings.add(new ModuleMarker(operationCallExpression,
						"Operation " + nameExpression.getName() + " is undefined for type " + currentNode.getName(),
						Severity.Warning));
			}
		}
	}

	@Override
	public void visit(OrOperatorExpression orOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) orOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(Parameter parameter) {
		if (context.getFrameStack().contains(parameter.getName()))
			visit(parameter, false);
		else
			visit(parameter, true);
	}

	public void visit(Parameter parameter, boolean createVariable) {
		if (parameter.getTypeExpression() != null) {
			parameter.getTypeExpression().accept(this);
		}
		if (createVariable) {
			context.getFrameStack().put(new Variable(parameter.getName(), getType(parameter)));
		}
	}

	@Override
	public void visit(PlusOperatorExpression plusOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) plusOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(PostfixOperatorExpression postfixOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) postfixOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(PropertyCallExpression propertyCallExpression) {
		Expression targetExpression = propertyCallExpression.getTargetExpression();
		NameExpression nameExpression = propertyCallExpression.getNameExpression();
		targetExpression.accept(this);

		// Extended properties
		if (nameExpression.getName().startsWith("~")) {
			setResolvedType(propertyCallExpression, EolAnyType.Instance);
		}
		// e.g. EPackage.all
		else if (targetExpression instanceof NameExpression && ((NameExpression) targetExpression).isTypeName()) {
			if (getResolvedType(((NameExpression) targetExpression)) instanceof EolModelElementType) {
				if (nameExpression.getName().equals("all") || nameExpression.getName().equals("allInstances")) {
					setResolvedType(propertyCallExpression,
							new EolCollectionType("Sequence", getResolvedType(targetExpression)));

				} else {
					EolType type = getResolvedType(targetExpression);

					boolean many = false;
					MetaClass metaClass = null;
					if (type instanceof EolModelElementType && ((EolModelElementType) type).getMetaClass() != null) {
						metaClass = (MetaClass) ((EolModelElementType) type).getMetaClass();
					} else if (type instanceof EolCollectionType
							&& ((EolCollectionType) type).getContentType() instanceof EolModelElementType) {
						metaClass = ((EolModelElementType) ((EolCollectionType) type).getContentType()).getMetaClass();
						many = true;
					}

					if (metaClass != null) {
						StructuralFeature structuralFeature = metaClass.getStructuralFeature(nameExpression.getName());
						if (structuralFeature != null) {
							if (structuralFeature.isMany()) {
								EolCollectionType collectionType = null;
								if (structuralFeature.isOrdered()) {
									if (structuralFeature.isUnique())
										collectionType = new EolCollectionType("OrderedSet");
									else
										collectionType = new EolCollectionType("Sequence");
								} else {
									if (structuralFeature.isUnique())
										collectionType = new EolCollectionType("Set");
									else
										collectionType = new EolCollectionType("Bag");
								}
								collectionType.setContentType(toStaticAnalyserType(structuralFeature.getType()));
								setResolvedType(propertyCallExpression, collectionType);
							} else {
								setResolvedType(propertyCallExpression,
										toStaticAnalyserType(structuralFeature.getType()));
							}
							if (many) {
								setResolvedType(propertyCallExpression,
										new EolCollectionType("Sequence", getResolvedType(propertyCallExpression)));
							}
						} else {
							errors.add(new ModuleMarker(nameExpression, "Structural feature " + nameExpression.getName()
									+ " not found in type " + metaClass.getName(), Severity.Error));
						}
					}

				}
			}
		}
		// Regular properties
		else {
			EolType type = getResolvedType(targetExpression);

			boolean many = false;
			MetaClass metaClass = null;
			if (type instanceof EolModelElementType && ((EolModelElementType) type).getMetaClass() != null) {
				metaClass = (MetaClass) ((EolModelElementType) type).getMetaClass();
			} else if (type instanceof EolCollectionType
					&& ((EolCollectionType) type).getContentType() instanceof EolModelElementType) {
				metaClass = ((EolModelElementType) ((EolCollectionType) type).getContentType()).getMetaClass();
				many = true;
			}

			if (metaClass != null) {
				StructuralFeature structuralFeature = metaClass.getStructuralFeature(nameExpression.getName());
				if (structuralFeature != null) {
					if (structuralFeature.isMany()) {
						String collectionTypeName;
						if (structuralFeature.isOrdered()) {
							collectionTypeName = structuralFeature.isUnique() ? "OrderedSet" : "Sequence";
						} else {
							collectionTypeName = structuralFeature.isUnique() ? "Set" : "Bag";
							if (structuralFeature.isConcurrent()) {
								collectionTypeName = "Concurrent" + collectionTypeName;
							}
						}
						setResolvedType(propertyCallExpression, new EolCollectionType(collectionTypeName));
						((EolCollectionType) getResolvedType(propertyCallExpression))
								.setContentType(toStaticAnalyserType(structuralFeature.getType()));
					} else {
						setResolvedType(propertyCallExpression, toStaticAnalyserType(structuralFeature.getType()));
					}
					if (many) {
						setResolvedType(propertyCallExpression,
								new EolCollectionType("Sequence", getResolvedType(propertyCallExpression)));
					}

				} else {
					errors.add(new ModuleMarker(nameExpression, "Structural feature " + nameExpression.getName()
							+ " not found in type " + metaClass.getName(), Severity.Error));
				}
			}

		}

	}

	@Override
	public void visit(RealLiteral realLiteral) {
		setResolvedType(realLiteral, EolPrimitiveType.Real);
	}

	@Override
	public void visit(ReturnStatement returnStatement) {
		Expression returnedExpression = returnStatement.getReturnedExpression();
		if (returnedExpression != null) {

			returnedExpression.accept(this);
			EolType providedReturnType = getResolvedType(returnedExpression);

			ModuleElement parent = returnedExpression.getParent();

			while (!(parent instanceof Operation) && parent != null) {

				parent = parent.getParent();

			}

			if (parent instanceof Operation) {
				setReturnFlag(((Operation) parent), true);
				EolType requiredReturnType = (EolType) parent.getData().get("returnType");

				if (!requiredReturnType.isAncestorOf(providedReturnType)) {
					if (canBeCompatible(requiredReturnType, providedReturnType))
						warnings.add(new ModuleMarker(returnedExpression, "Return type might be " + requiredReturnType
								+ " instead of " + getResolvedType(returnedExpression), Severity.Warning));
					else
						errors.add(new ModuleMarker(returnedExpression, "Return type should be " + requiredReturnType
								+ " instead of " + getResolvedType(returnedExpression), Severity.Error));

				}
			}
		}

	}

	@Override
	public void visit(SimpleAnnotation simpleAnnotation) {
	}

	@Override
	public void visit(StatementBlock statementBlock) {

		statementBlock.getStatements().forEach(s -> s.accept(this));

	}

	@Override
	public void visit(StringLiteral stringLiteral) {
		setResolvedType(stringLiteral, EolPrimitiveType.String);
	}

	@Override
	public void visit(SwitchStatement switchStatement) {
	}

	@Override
	public void visit(TernaryExpression ternaryExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) ternaryExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(ThrowStatement throwStatement) {
		throwStatement.getThrown().accept(this);
	}

	@Override
	public void visit(TimesOperatorExpression timesOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) timesOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(TransactionStatement transactionStatement) {
	}

	@Override
	public void visit(TypeExpression typeExpression) {
		EolType type = toStaticAnalyserType(TypeExpression.getType(typeExpression.getName()));

		for (TypeExpression typeExp : typeExpression.getParameterTypeExpressions()) {
			typeExp.accept(this);
		}

		if (type instanceof EolPrimitiveType) {
			setResolvedType(typeExpression, type);
		}

		if (type == null) {

			switch (typeExpression.getName()) {
			case "EolSelf":
				type = new EolSelf();
				setResolvedType(typeExpression, type);
				break;
			case "EolSelfContentType":
				type = new EolSelfContentType();
				setResolvedType(typeExpression, type);
				break;
			case "EolSelfExpressionType":
				type = new EolSelfExpressionType();
				setResolvedType(typeExpression, type);
				break;
			case "EolSelfCollectionType":
				type = new EolSelfCollectionType();
				setResolvedType(typeExpression, type);
				break;
			}
		}

		if (type instanceof EolCollectionType) {
			setResolvedType(typeExpression, type);
			if (typeExpression.getParameterTypeExpressions().size() == 1) {
				((EolCollectionType) type)
						.setContentType(getResolvedType(typeExpression.getParameterTypeExpressions().get(0)));
				setResolvedType(typeExpression, type);
			} else if (typeExpression.getParameterTypeExpressions().size() > 1) {
				errors.add(new ModuleMarker(typeExpression, "Collection types can have at most one content type",
						Severity.Error));
			}
		}

		if (type instanceof EolMapType) {
			if (typeExpression.getParameterTypeExpressions().size() == 2) {
				((EolMapType) type).setKeyType(getResolvedType(typeExpression.getParameterTypeExpressions().get(0)));
				((EolMapType) type).setValueType(getResolvedType(typeExpression.getParameterTypeExpressions().get(1)));
			} else if (typeExpression.getParameterTypeExpressions().size() > 0) {
				errors.add(new ModuleMarker(typeExpression, "Maps need two types: key-type and value-type",
						Severity.Error));
			}
		}

		if (type == null) {
			// TODO: Remove duplication between this and NameExpression
			EolModelElementType modelElementType = getModelElementType(typeExpression.getName(), typeExpression);
			if (modelElementType != null) {
				type = modelElementType;
				if (modelElementType.getMetaClass() == null && !context.getModelDeclarations().isEmpty()) {
					errors.add(new ModuleMarker(typeExpression, "Unknown type " + typeExpression.getName(),
							Severity.Error));
				}
			} else {
				errors.add(new ModuleMarker(typeExpression, "Undefined variable or type " + typeExpression.getName(),
						Severity.Error));
			}
		}
		if (type instanceof EolModelElementType)
			setResolvedType(typeExpression, type);

	}

	@Override
	public void visit(VariableDeclaration variableDeclaration) {

		EolType type;
		TypeExpression typeExpression = variableDeclaration.getTypeExpression();

		if (typeExpression != null) {
			typeExpression.accept(this);
			type = getResolvedType(typeExpression);
		} else {
			type = EolAnyType.Instance;
		}

		if (context.getFrameStack().getTopFrame().contains(variableDeclaration.getName())) {
			errors.add(new ModuleMarker(variableDeclaration,
					"Variable " + variableDeclaration.getName() + " has already been defined", Severity.Error));
		} else {
			context.getFrameStack().put(new Variable(variableDeclaration.getName(), type));
			setResolvedType(variableDeclaration, type);
		}

	}

	@Override
	public void visit(WhileStatement whileStatement) {

		FrameStack frameStack = context.getFrameStack();
		Expression conditionExpression = whileStatement.getConditionExpression();
		StatementBlock bodyStatementBlock = whileStatement.getBodyStatementBlock();

		conditionExpression.accept(this);

		frameStack.enterLocal(FrameType.UNPROTECTED, bodyStatementBlock);
		bodyStatementBlock.accept(this);
		;
		frameStack.leaveLocal(bodyStatementBlock);

		if (hasResolvedType(conditionExpression) && getResolvedType(conditionExpression) != EolPrimitiveType.Boolean) {
			errors.add(new ModuleMarker(conditionExpression, "Condition must be a Boolean", Severity.Error));
		}
	}

	@Override
	public void visit(XorOperatorExpression xorOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) xorOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	public void operationPreVisitor(Operation operation) {
		TypeExpression contextTypeExpression = operation.getContextTypeExpression();
		EolType contextType = EolNoType.Instance;
		TypeExpression returnTypeExpression = operation.getReturnTypeExpression();
		EolType returnType = EolAnyType.Instance;

		if (contextTypeExpression != null) {
			contextTypeExpression.accept(this);
			contextType = getResolvedType(contextTypeExpression);
		}

		if (returnTypeExpression != null) {
			returnTypeExpression.accept(this);
			returnType = getResolvedType(returnTypeExpression);
		}

		operation.getData().put("contextType", contextType);
		operation.getData().put("returnType", returnType);
	}

	public void preValidate(IEolModule imodule) {

		EolModule eolModule = (EolModule) imodule;
		this.module = eolModule;

		for (ModelDeclaration modelDeclaration : module.getDeclaredModelDeclarations()) {
			modelDeclaration.accept(this);
		}

		module.getDeclaredOperations().forEach(o -> operationPreVisitor(o));
		module.getDeclaredOperations().forEach(o -> operations.add(new SimpleOperation(o)));
		module.getDeclaredOperations().forEach(o -> o.accept(this));

		// Parse builtin operations
		List<OperationContributor> operationContributors = context.operationContributorRegistry.stream()
				.collect(Collectors.toList());
		for (OperationContributor oc : operationContributors) {
			EolType contextType = oc.contributesToType();

			for (Method m : oc.getClass().getDeclaredMethods()) {
				List<EolType> operationParameterTypes = new ArrayList<EolType>();
				Type[] javaParameterTypes = m.getGenericParameterTypes();
				for (Type javaParameterType : javaParameterTypes) {
					operationParameterTypes.add(javaTypeToEolType(javaParameterType));
				}
				EolType returnType = javaTypeToEolType(m.getGenericReturnType());
				operations.add(new SimpleOperation(m.getName(), contextType, returnType, operationParameterTypes));
			}
		}
	}

	public EolType javaTypeToEolType(Type javaType) {

		if (javaType instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) javaType).getRawType();
			Type[] typeArgs = ((ParameterizedType) javaType).getActualTypeArguments();
			EolType eolType = javaClassToEolType((Class<?>) rawType);
			if (eolType instanceof EolCollectionType) {
				EolType contentType = javaTypeToEolType(typeArgs[0]);
				eolType = new EolCollectionType(eolType.getName(), contentType);
				return eolType;
			} else if (eolType instanceof EolMapType) {
				EolType keyType = javaTypeToEolType(typeArgs[0]);
				EolType valueType = javaTypeToEolType(typeArgs[1]);
				eolType = new EolMapType(keyType, valueType);
				return eolType;
			} else {
				return EolAnyType.Instance;
			}
		} else if (javaType instanceof Class<?>) {
			Class<?> javaClass = (Class<?>) javaType;
			return javaClassToEolType(javaClass);
		} else {
			return EolAnyType.Instance;
		}
	}

	public EolType javaClassToEolType(Class<?> javaClass) {
		if (javaClass == String.class || javaClass == char.class) {
			return EolPrimitiveType.String;
		} else if (javaClass == Integer.class || javaClass == int.class) {
			return EolPrimitiveType.Integer;
		} else if (javaClass == Double.class || javaClass == double.class || javaClass == Float.class
				|| javaClass == float.class) {
			return EolPrimitiveType.Real;
		} else if (javaClass == boolean.class || javaClass == Boolean.class) {
			return EolPrimitiveType.Boolean;
		} else if (javaClass == java.util.Collection.class) {
			return EolCollectionType.Bag;
		} else if (javaClass == java.util.List.class) {
			return EolCollectionType.Sequence;
		} else if (javaClass == java.util.Set.class) {
			return EolCollectionType.Set;
		} else if (javaClass == java.util.Map.class) {
			return EolMapType.Map;
		} else if (javaClass == org.eclipse.epsilon.eol.types.EolCollection.class) {
			return EolCollectionType.Collection;
		} else if (javaClass == org.eclipse.epsilon.eol.types.EolBag.class) {
			return EolCollectionType.Bag;
		} else if (javaClass == org.eclipse.epsilon.eol.types.EolSequence.class) {
			return EolCollectionType.Sequence;
		} else if (javaClass == org.eclipse.epsilon.eol.types.EolSet.class) {
			return EolCollectionType.Set;
		} else if (javaClass == org.eclipse.epsilon.eol.types.EolOrderedSet.class) {
			return EolCollectionType.OrderedSet;
		} else if (javaClass == org.eclipse.epsilon.eol.types.concurrent.EolConcurrentBag.class) {
			return EolCollectionType.ConcurrentBag;
		} else if (javaClass == org.eclipse.epsilon.eol.types.concurrent.EolConcurrentSet.class) {
			return EolCollectionType.ConcurrentSet;
		} else if (javaClass == org.eclipse.epsilon.eol.types.concurrent.EolConcurrentMap.class) {
			return EolMapType.ConcurrentMap;
		} else {
			return new EolNativeType(javaClass);
		}
	}

	public void mainValidate(IEolModule module) {

		if (module.getMain() != null)
			module.getMain().accept(this);
	}

	public void postValidate(IEolModule module) {
		context.getFrameStack().dispose();
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		errors = new ArrayList<ModuleMarker>();
		warnings = new ArrayList<ModuleMarker>();
		List<ModuleMarker> markers = new ArrayList<ModuleMarker>();
		EolModule eolModule = (EolModule) imodule;
		this.module = eolModule;

		preValidate(module);
		mainValidate(module);
		postValidate(module);
		markers.addAll(errors);
		markers.addAll(warnings);
		return markers;
	}

	@Override
	public String getMarkerType() {
		return ModuleMarker.Severity.Error.name();
	}

	public void createTypeCompatibilityWarning(Expression requiredExpression, Expression providedExpression) {
		warnings.add(new ModuleMarker(providedExpression,
				getResolvedType(providedExpression) + " may not be assigned to " + getResolvedType(requiredExpression),
				Severity.Warning));
	}

	public void createTypeCompatibilityError(Expression requiredExpression, Expression providedExpression) {
		errors.add(new ModuleMarker(providedExpression,
				getResolvedType(providedExpression) + " cannot be assigned to " + getResolvedType(requiredExpression),
				Severity.Error));
	}

	public boolean canBeCompatible(EolType targetType, EolType valueType) {

		if (targetType == null || valueType == null) {
			return false;
		}

		if (valueType instanceof EolUnionType) {
			EolType tempType;
			for (EolType t : ((EolUnionType) valueType).containedTypes) {
				tempType = targetType;
				while (true) {
					if (tempType instanceof EolAnyType) {
						break;
					}
					if (t.equals(tempType)) {
						return true;

					} else {
						tempType = getParentType(tempType);
					}
				}
			}
			return false;
		}

		if (targetType instanceof EolUnionType) {
			for (EolType t : ((EolUnionType) targetType).containedTypes) {
				if (canBeCompatible(t, valueType)) {
					return true;
				}
			}
			return false;
		}

		while (true) {

			if (!(targetType.equals(valueType)) && !(valueType instanceof EolAnyType)) {

				targetType = getParentType(targetType);

				if (targetType instanceof EolAnyType) {
					return false;
				}

			} else if (valueType instanceof EolAnyType) {
				return true;
			} else if (targetType instanceof EolCollectionType
					&& !((((EolCollectionType) valueType).getContentType()) instanceof EolAnyType)) {

				EolType valueContentType = ((EolCollectionType) valueType).getContentType();
				EolType targetContentType = ((EolCollectionType) targetType).getContentType();

				while (targetContentType instanceof EolCollectionType
						&& valueContentType instanceof EolCollectionType) {
					if (targetContentType.equals(valueContentType)) {
						return canBeCompatible(((EolCollectionType) targetContentType).getContentType(),
								((EolCollectionType) valueContentType).getContentType());
					} else {
						valueContentType = getParentType(valueContentType);
						return canBeCompatible(targetContentType, valueContentType);

					}
				}
				while (true) {
					if (valueContentType instanceof EolAnyType || targetContentType instanceof EolAnyType) {
						return true;
					}
					if (!valueContentType.equals(targetContentType)) {
						targetContentType = getParentType(targetContentType);
						if (targetContentType instanceof EolAnyType)
							return false;
					} else {
						return true;
					}
				}
			} else {
				return true;
			}
		}
	}

	public void visitOperatorExpression(OperatorExpression operatorExpression) {
		Expression firstOperand = operatorExpression.getFirstOperand();
		Expression secondOperand = operatorExpression.getSecondOperand();
		String operator = operatorExpression.getOperator();
		List<Expression> operands = operatorExpression.getOperands();

		firstOperand.accept(this);
		if (secondOperand != null)
			secondOperand.accept(this);
		List<EolType> operandTypes = operands.stream().map(o -> getResolvedType(o)).collect(Collectors.toList());

		if (StringUtil.isOneOf(operator, "and", "or", "xor", "not", "implies")) {
			for (Expression operand : operatorExpression.getOperands()) {
				if (hasResolvedType(operand) && getResolvedType(operand) != EolPrimitiveType.Boolean) {
					errors.add(new ModuleMarker(operatorExpression,
							"Boolean expected instead of " + getResolvedType(operand), Severity.Error));
				}
			}
			setResolvedType(operatorExpression, EolPrimitiveType.Boolean);
		}

		if (StringUtil.isOneOf(operator, "<", ">", ">=", "<=", "*", "/", "-")) {
			for (Expression operand : operands) {
				if (hasResolvedType(operand) && getResolvedType(operand) != EolPrimitiveType.Integer
						&& getResolvedType(operand) != EolPrimitiveType.Real) {
					setResolvedType(operatorExpression, EolAnyType.Instance);
					errors.add(new ModuleMarker(operatorExpression,
							"Number expected instead of " + getResolvedType(operand), Severity.Error));
				} else if (StringUtil.isOneOf(operator, "*", "/", "-")) {
					if (getResolvedType(operand) == EolPrimitiveType.Real)
						setResolvedType(operatorExpression, EolPrimitiveType.Real);
					else
						setResolvedType(operatorExpression, EolPrimitiveType.Integer);
				}
			}
		}

		if (StringUtil.isOneOf(operator, "==", "=", "<>", "<", ">", ">=", "<=")) {
			setResolvedType(operatorExpression, EolPrimitiveType.Boolean);
		}

		if (StringUtil.isOneOf(operator, "+")) {
			if (operandTypes.stream().allMatch(
					t -> t.equals(EolNativeType.Number) || t.equals(EolPrimitiveType.Real) || t.equals(EolPrimitiveType.Integer))) {
				if (operandTypes.contains(EolNativeType.Number))
					setResolvedType(operatorExpression, EolNativeType.Number);
				else if (operandTypes.contains(EolPrimitiveType.Real))
					setResolvedType(operatorExpression, EolPrimitiveType.Real);
				else
					setResolvedType(operatorExpression, EolPrimitiveType.Integer);
			}else if (operandTypes.stream().allMatch(t -> t instanceof EolCollectionType))
				setResolvedType(operatorExpression, operandTypes.get(0));
			else 
				setResolvedType(operatorExpression, EolPrimitiveType.String);
		}
	}

	public boolean hasReturnStatement(Operation operation) {
		ArrayList<ModuleElement> statements = new ArrayList<ModuleElement>();
		statements.addAll(operation.getBody().getChildren());

		while (!(statements.isEmpty())) {
			ModuleElement st = statements.get(0);
			statements.remove(st);
			if (!(st.getChildren().isEmpty()))
				statements.addAll(st.getChildren());
			if (st instanceof ReturnStatement)
				return true;
		}
		return false;
	}

	public boolean getReturnFlag(Operation op) {
		return returnFlags.containsKey(op) ? returnFlags.get(op) : false;
	}

	public void setReturnFlag(Operation op, boolean returnFlag) {
		returnFlags.put(op, returnFlag);
	}

	public void setResolvedType(Expression expression, EolType type) {
		expression.getData().put("resolvedType", type);
	}

	public EolType getResolvedType(Expression expression) {
		EolType resolvedType = (EolType) expression.getData().get("resolvedType");
		if (resolvedType == null) {
			resolvedType = EolAnyType.Instance;
			setResolvedType(expression, resolvedType);
		}
		return resolvedType;
	}

	public boolean hasResolvedType(Expression expresion) {
		EolType resolvedType = getResolvedType(expresion);
		return resolvedType != EolAnyType.Instance;
	}

	public EolType getType(Parameter parameter) {
		EolType type = (EolType) parameter.getData().get("type");
		if (type == null) {
			if (parameter.getTypeExpression() != null) {
				type = getResolvedType(parameter.getTypeExpression());
			} else {
				type = EolAnyType.Instance;
			}
			setType(parameter, type);
		}
		return type;
	}

	public void setType(Parameter parameter, EolType type) {
		parameter.getData().put("type", type);
	}

	/**
	 * TODO: We should be considering all parent types - not just the first one
	 */
	public EolType getParentType(EolType type) {
		if (type.getParentTypes().isEmpty())
			return null;
		else
			return type.getParentTypes().get(0);
	}

	public EolStaticAnalysisContext getContext() {
		return context;
	}

	public void setContext(EolStaticAnalysisContext context) {
		this.context = context;
	}

	public EolModelElementType getModelElementType(String modelAndType, AbstractModuleElement element) {

		String modelName;
		String typeName;
		if (modelAndType.contains("!")) {
			modelName = modelAndType.split("!")[0];
			typeName = modelAndType.split("!")[1];
		} else {
			modelName = "";
			typeName = modelAndType;
		}

		IModel model = context.repository.getModelByNameSafe(modelName);
		if (model == null) {
			return null;
		}

		if (model instanceof ModelGroup) {
			TypeAmbiguityCheckResult result = context.repository.checkAmbiguity(typeName);
			if (result.namesOfOwningModels.size() == 0) {
				return null;
			}

			modelName = result.nameOfSelectedModel;
			if (result.isAmbiguous) {
				warnings.add(new ModuleMarker(element,
						"Ambiguous type, consider using a concrete model name istead of an alias", Severity.Warning));
			}

		}
		if (modelName == "") {
			modelName = model.getName();
		}
		Metamodel metamodel = context.modelDeclarations.get(modelName).getMetamodel();
		if (metamodel != null) {
			EolModelElementType modelElementType = new EolModelElementType(modelAndType, module);
			modelElementType.setMetaClass(metamodel.getMetaClass(typeName));
			return modelElementType;
		} else {
			return null;
		}

	}

	private EolType toStaticAnalyserType(org.eclipse.epsilon.eol.types.EolType type) {
		if (type == null) {
			return null;
		} else if (type instanceof org.eclipse.epsilon.eol.types.EolModelElementType) {
			org.eclipse.epsilon.eol.types.EolModelElementType type2 = (org.eclipse.epsilon.eol.types.EolModelElementType) type;
			String modelAndMetaClass = type2.getModelName().equals("") ? type2.getMetaClass().getName()
					: type2.getModelName() + "!" + type2.getMetaClass().getName();
			EolModelElementType newType = new EolModelElementType(modelAndMetaClass, this.module);
			newType.setMetaClass(type2.getMetaClass());
			return newType;
		} else {
			String name = type.getName();
			switch (name) {
			case "Integer":
				return EolPrimitiveType.Integer;
			case "Any":
				return EolAnyType.Instance;
			case "Boolean":
				return EolPrimitiveType.Boolean;
			case "String":
				return EolPrimitiveType.String;
			case "Real":
				return EolPrimitiveType.Real;
			case "Map":
			case "ConcurrentMap":
				return new EolMapType(name);
			case "List":
				name = "Sequence";
			case "Bag":
			case "Collection":
			case "ConcurrentBag":
			case "ConcurrentSet":
			case "OrderedSet":
			case "Sequence":
			case "Set":
				return new EolCollectionType(name);
			case "Nothing":
			case "None":
				return EolNoType.Instance;
			case "Tuple":
				return new EolTupleType();
			default:
				return null;
			}
		}

	}

}