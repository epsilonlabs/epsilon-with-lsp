package org.eclipse.epsilon.eol.staticanalyser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.IModuleValidator;
import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.common.parse.Position;
import org.eclipse.epsilon.common.parse.Region;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.common.util.StringUtil;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.dom.AbortStatement;
import org.eclipse.epsilon.eol.dom.AbstractExecutableModuleElement;
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
import org.eclipse.epsilon.eol.dom.Statement;
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
import org.eclipse.epsilon.eol.staticanalyser.execute.context.SingleFrame;
import org.eclipse.epsilon.eol.execute.context.FrameType;
import org.eclipse.epsilon.eol.execute.operations.AbstractOperation;
import org.eclipse.epsilon.eol.execute.operations.MethodDiagnosticsCalculator;
import org.eclipse.epsilon.eol.execute.operations.MethodTypeCalculator;
import org.eclipse.epsilon.eol.execute.operations.TypeCalculator;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributor;
import org.eclipse.epsilon.eol.execute.operations.declarative.FirstOrderOperation;
import org.eclipse.epsilon.eol.staticanalyser.execute.context.Variable;
import org.eclipse.epsilon.eol.m3.IEnum;
import org.eclipse.epsilon.eol.m3.IMetaClass;
import org.eclipse.epsilon.eol.m3.IMetamodel;
import org.eclipse.epsilon.eol.m3.IProperty;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.ModelGroup;
import org.eclipse.epsilon.eol.models.ModelRepository.TypeAmbiguityCheckResult;
import org.eclipse.epsilon.eol.models.UnknownModel;
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
import org.eclipse.epsilon.eol.staticanalyser.types.EolTypeLiteral;

public class EolStaticAnalyser implements IModuleValidator, IEolVisitor {

	private static final String DEFAULT_MODEL_NAME = "";
	private static final String UNKNOWN_MODEL_DRIVER = "Unknown";
	private static final String[] BUILTIN_TYPE_COMPLETION_NAMES = new String[] {
			"Any", "Bag", "Boolean", "Collection", "ConcurrentBag", "ConcurrentMap", "ConcurrentSet",
			"Integer", "List", "Map", "Native", "None", "Nothing", "OrderedSet", "Real", "Sequence", "Set",
			"String", "Tuple" };

	protected List<ModuleMarker> markers = new ArrayList<>();
	protected IEolModule module;
	protected EolStaticAnalysisContext context = new EolStaticAnalysisContext();
	protected List<IStaticOperation> localOperations = new ArrayList<>();
	protected List<IStaticOperation> importedOperations = new ArrayList<>();
	protected List<IStaticOperation> builtinOperations = new ArrayList<>();
	HashMap<Operation, Boolean> returnFlags = new HashMap<>(); // for every missmatch
	HashMap<URI, List<IStaticOperation>> operationRegistry = new HashMap<>();
//	List<EolModule> dependencies = new ArrayList<>();

	/**
	 * Registry of visible variable snapshots captured during validation. Each
	 * entry maps an AST element's source {@link Region} to the variables that
	 * are in scope at that region. Used by {@link #getCompletions} to serve
	 * autocomplete lookups without re-walking the AST.
	 */
	protected List<VisibleVariablesSnapshot> visibleVariablesRegistry = new ArrayList<>();

	/**
	 * A snapshot of the variables visible at the source region of a given AST
	 * element, taken while the static analyser is visiting that element.
	 */
	public static class VisibleVariablesSnapshot {
		public final Region region;
		public final Map<String, Variable> variables;
		public final boolean topLevelScope;

		public VisibleVariablesSnapshot(Region region, Map<String, Variable> variables) {
			this(region, variables, false);
		}

		public VisibleVariablesSnapshot(Region region, Map<String, Variable> variables, boolean topLevelScope) {
			this.region = region;
			this.variables = variables;
			this.topLevelScope = topLevelScope;
		}
	}

	protected static class MemberCompletionContext {
		public final NameExpression nameExpression;
		public final EolType targetType;
		public final boolean operationCall;
		public final boolean typeLiteralTarget;

		public MemberCompletionContext(NameExpression nameExpression, EolType targetType, boolean operationCall,
				boolean typeLiteralTarget) {
			this.nameExpression = nameExpression;
			this.targetType = targetType;
			this.operationCall = operationCall;
			this.typeLiteralTarget = typeLiteralTarget;
		}

		public Region getNameRegion() {
			return nameExpression != null ? nameExpression.getRegion() : null;
		}
	}

	protected static class FirstOrderStaticOperation implements IStaticOperation {
		private final String name;
		private final AbstractOperation operation;

		public FirstOrderStaticOperation(String name, AbstractOperation operation) {
			this.name = name;
			this.operation = operation;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public EolType getContextType() {
			return EolCollectionType.Collection;
		}

		@Override
		public EolType getReturnType(EolType actualContextType, List<EolType> actualParameterTypes) {
			TypeCalculator typeCalculator = operation.getClass().getAnnotation(TypeCalculator.class);
			if (typeCalculator == null) {
				return EolAnyType.Instance;
			}

			EolType contextType = actualContextType;
			if (!(contextType instanceof EolCollectionType)) {
				contextType = new EolCollectionType("Sequence", contextType);
			}
			EolType iteratorType = ((EolCollectionType) contextType).getContentType();
			List<EolType> expressionTypes = new ArrayList<EolType>();
			if (actualParameterTypes != null) {
				expressionTypes.addAll(actualParameterTypes);
			}
			while (expressionTypes.size() < 2) {
				expressionTypes.add(EolAnyType.Instance);
			}

			try {
				return typeCalculator.klass().newInstance().calculateType(contextType, iteratorType, expressionTypes);
			}
			catch (Exception e) {
				return EolAnyType.Instance;
			}
		}

		@Override
		public List<EolType> getParameterTypes() {
			return Collections.emptyList();
		}

		@Override
		public List<String> getParameterNames() {
			return Collections.emptyList();
		}

		@Override
		public boolean isVarArgs() {
			return false;
		}

		@Override
		public List<ModuleMarker> getExtraDiagnostics(AbstractModuleElement element, EolType actualContextType,
				List<EolType> actualParameterTypes) {
			return Collections.emptyList();
		}
	}

	protected static class TypeCompletionContext {
		public final String modelName;
		public final String[] packageNames;
		public final String prefix;
		public final boolean typeExpression;

		public TypeCompletionContext(String modelName, String[] packageNames, String prefix, boolean typeExpression) {
			this.modelName = modelName;
			this.packageNames = packageNames;
			this.prefix = prefix != null ? prefix : "";
			this.typeExpression = typeExpression;
		}

		public boolean hasPackageQualifier() {
			return packageNames.length > 0;
		}
	}

	protected static class EnumerationLiteralCompletionContext {
		public final EnumerationLiteralExpression expression;
		public final String enumerationName;
		public final String prefix;

		public EnumerationLiteralCompletionContext(EnumerationLiteralExpression expression, String enumerationName,
				String prefix) {
			this.expression = expression;
			this.enumerationName = enumerationName;
			this.prefix = prefix != null ? prefix : "";
		}

		public Region getRegion() {
			return expression != null ? expression.getRegion() : null;
		}
	}

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

		// If the target is a property call on a Tuple, pre-register the property type
		// before visiting the target so the PropertyCallExpression visitor finds it
		if (targetExpression instanceof PropertyCallExpression) {
			PropertyCallExpression pce = (PropertyCallExpression) targetExpression;
			pce.getTargetExpression().accept(this);
			EolType ownerType = getResolvedType(pce.getTargetExpression());
			EolType valueType = getResolvedType(valueExpression);
			if (ownerType instanceof EolTupleType && !(valueType.equals(EolAnyType.Instance))) {
				String propertyName = pce.getNameExpression().getName();
				((EolTupleType) ownerType).setPropertyType(propertyName, valueType);
			}
		}

		targetExpression.accept(this);

		EolType targetType = getResolvedType(targetExpression);
		EolType valueType = getResolvedType(valueExpression);

		if (targetExpression instanceof VariableDeclaration && targetType instanceof EolTupleType
				&& valueType instanceof EolTupleType) {
			EolTupleType targetTupleType = (EolTupleType) targetType;
			for (Map.Entry<String, EolType> property : ((EolTupleType) valueType).getPropertyTypes().entrySet()) {
				targetTupleType.setPropertyType(property.getKey(), property.getValue());
			}
		}

		if (targetType instanceof EolModelElementType && ((EolModelElementType) targetType).getMetaClass() != null)
			targetType = new EolModelElementType(((EolModelElementType) targetType).getMetaClass());
		if (valueType instanceof EolModelElementType && ((EolModelElementType) valueType).getMetaClass() != null)
			valueType = new EolModelElementType(((EolModelElementType) valueType).getMetaClass());

		if (!valueType.isAssignableTo(targetType)) {
			if (targetType.isAssignableTo(valueType)) {
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
		Expression condition = case_.getCondition();
		if (condition != null) {
			condition.accept(this);
		}
		StatementBlock body = case_.getBody();
		if (body != null) {
			body.accept(this);
		}
	}

	@Override
	public void visit(CollectionLiteralExpression<?> collectionLiteralExpression) {
		if (!collectionLiteralExpression.getParameterExpressions().isEmpty()) {
			Set<EolType> types = new LinkedHashSet<EolType>();
			for (Expression e : collectionLiteralExpression.getParameterExpressions()) {
				e.accept(this);
				types.add(getResolvedType(e));
			}
			if (types.size() == 1) {
				setResolvedType(collectionLiteralExpression, new EolCollectionType(
						collectionLiteralExpression.getCollectionType(), types.iterator().next()));
			}
			else {
				setResolvedType(collectionLiteralExpression,
						new EolCollectionType(collectionLiteralExpression.getCollectionType(),
								new EolUnionType(types)));

			}
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
		String enumeration = enumerationLiteralExpression.getEnumerationLiteral();
		// split at the # to get the enum name and literal
		int hashIndex = enumeration.indexOf('#');
		String enumName = enumeration.substring(0, hashIndex);
		String literalName = enumeration.substring(hashIndex + 1);

		EolModelElementType enumType = getModelElementType(enumName, enumerationLiteralExpression);
		if (enumType == null) {
			if (modelHasNoMetamodel(enumName)) {
				setResolvedType(enumerationLiteralExpression, EolAnyType.Instance);
			} else {
				markers.add(new ModuleMarker(enumerationLiteralExpression, "Undefined enumeration type " + enumName,
						Severity.Error));
			}
			return;
		} else {
			IMetaClass metaClass = enumType.getMetaClass();
			if (!(metaClass instanceof IEnum)) {
				markers.add(new ModuleMarker(enumerationLiteralExpression, enumName + " is not an enumeration type",
						Severity.Error));
				return;
			} else {
				IEnum enumerationType = (IEnum) metaClass;
				if (!enumerationType.isValidEnumLiteral(literalName)) {
					markers.add(new ModuleMarker(enumerationLiteralExpression,
							"Undefined enumeration literal " + literalName + " for enumeration " + enumName,
							Severity.Error));
					return;
				}
			}
		}
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
			setResolvedType(executableBlock, getResolvedType((Expression)body));
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
			markers.add(new ModuleMarker(firstOrderOperationCallExpression.getNameExpression(),
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
		if (iterator.getRegion() != null && iterator.getRegion().getEnd() != null
				&& firstOrderOperationCallExpression.getRegion() != null
				&& firstOrderOperationCallExpression.getRegion().getEnd() != null) {
			recordVisibleVariables(new Region(iterator.getRegion().getEnd(),
					firstOrderOperationCallExpression.getRegion().getEnd()));
		}
		List<Expression> expressions = firstOrderOperationCallExpression.getExpressions();
		for (Expression expression: expressions) {
			recordVisibleVariables(expression);
			expression.accept(this);
		}
		
		List<EolType> expressionTypes = expressions.stream().map(e -> getResolvedType(e)).collect(Collectors.toList());
		context.getFrameStack().leaveLocal(firstOrderOperationCallExpression);

		try {
			EolType returnType = tc.klass().newInstance().calculateType(contextType, iteratorType, expressionTypes);
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

		if (hasResolvedType(forStatement.getIteratedExpression())) {
			EolType iteratedType = getResolvedType(forStatement.getIteratedExpression());
			boolean isCollection = iteratedType.isAssignableTo(EolCollectionType.Collection);
			boolean isArray = false;
			boolean isIterable = false;
			
			if (iteratedType instanceof EolNativeType) {
				Class<?> clazz = ((EolNativeType) iteratedType).getClazz();
				isArray = clazz.isArray();
				isIterable = Iterable.class.isAssignableFrom(clazz);
			}

			if (!isCollection && !isArray && !isIterable) {
				markers.add(new ModuleMarker(forStatement.getIteratedExpression(),
						"Collection expected instead of " + iteratedType,
						Severity.Error));
			}
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
			markers.add(new ModuleMarker(conditionExpression, "Condition must be a Boolean", Severity.Error));
		}

	}

	@Override
	public void visit(ImpliesOperatorExpression impliesOperatorExpression) {
		OperatorExpression operatorExpression = (OperatorExpression) impliesOperatorExpression;
		visitOperatorExpression(operatorExpression);
	}

	@Override
	public void visit(Import import_) {
		
		EolModule importedModule = (EolModule) import_.getImportedModule();
		if(operationRegistry.getOrDefault(importedModule.getUri(), null) == null) {
			//Register an empty list to avoid cyclic imports
			operationRegistry.put(importedModule.getUri(), new ArrayList<>());
			preValidate(importedModule);
			//We do not care about errors from imported modules
			markers.clear();
		}
		importedOperations.addAll(operationRegistry.get(importedModule.getUri()));
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
				markers.add(new ModuleMarker(itemSelectorExpression.getIndexExpression(),
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
		if (mapLiteralExpression.isTuple()) {
			EolTupleType tupleType = new EolTupleType();
			for (Map.Entry<Expression, Expression> pair : mapLiteralExpression.getKeyValueExpressionPairs()) {
				pair.getValue().accept(this);
				EolType valueType = getResolvedType(pair.getValue());
				// Mirroring runtime: NameExpression keys are treated as string property names
				String propertyName = null;
				if (pair.getKey() instanceof NameExpression) {
					propertyName = ((NameExpression) pair.getKey()).getName();
					setResolvedType(pair.getKey(), EolPrimitiveType.String);
				} else {
					pair.getKey().accept(this);
					EolType keyType = getResolvedType(pair.getKey());
					if (keyType.equals(EolPrimitiveType.String) && pair.getKey() instanceof StringLiteral) {
						propertyName = ((StringLiteral) pair.getKey()).getValue();
					}
				}
				if (propertyName != null && !(valueType.equals(EolAnyType.Instance))) {
					tupleType.setPropertyType(propertyName, valueType);
				}
			}
			setResolvedType(mapLiteralExpression, tupleType);
		}
		else if (!mapLiteralExpression.getKeyValueExpressionPairs().isEmpty()) {
			Set<EolType> keyTypes = new LinkedHashSet<EolType>();
			Set<EolType> valueTypes = new LinkedHashSet<EolType>();
			for (Map.Entry<Expression, Expression> pair : mapLiteralExpression.getKeyValueExpressionPairs()) {
				pair.getKey().accept(this);
				pair.getValue().accept(this);
				keyTypes.add(getResolvedType(pair.getKey()));
				valueTypes.add(getResolvedType(pair.getValue()));
			}
			EolType keyType = keyTypes.size() == 1 ? keyTypes.iterator().next() : new EolUnionType(keyTypes);
			EolType valueType = valueTypes.size() == 1 ? valueTypes.iterator().next() : new EolUnionType(valueTypes);
			setResolvedType(mapLiteralExpression, new EolMapType(keyType, valueType));
		}
		else {
			setResolvedType(mapLiteralExpression, new EolMapType());
		}
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
					markers.add(new ModuleMarker(modelDeclaration, error, Severity.Error));
				}
				for (String warning : modelDeclaration.getMetamodel().getWarnings()) {
					markers.add(new ModuleMarker(modelDeclaration, warning, Severity.Warning));
				}
			} else {
				markers.add(new ModuleMarker(modelDeclaration,
						"Model driver '" + modelDeclaration.getDriverNameExpression().getName()
								+ "' does not provide a metamodel; type checking is unavailable for model '"
								+ modelName + "'",
						Severity.Warning));
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
		} else if (TypeExpression.getType(nameExpression.getName()) != null) {
			setResolvedType(nameExpression,
					new EolTypeLiteral(toStaticAnalyserType(TypeExpression.getType(nameExpression.getName()))));
		} else if(context.repository.getModelByNameSafe(nameExpression.getName()) != null){
			IModel m = context.repository.getModelByNameSafe(nameExpression.getName());
			setResolvedType(nameExpression, new EolNativeType(m.getClass()));
		} else {
			modelElementType = getModelElementType(nameExpression.getName(), nameExpression);
			if (modelElementType != null) {
				setResolvedType(nameExpression, new EolTypeLiteral(modelElementType));
				nameExpression.setTypeName(true);
				if (modelElementType.getMetaClass() == null && !context.getModelDeclarations().isEmpty()) {

					markers.add(new ModuleMarker(nameExpression, "Undefined variable or type " + nameExpression.getName(),
							Severity.Error));
				}

			} else if (modelHasNoMetamodel(nameExpression.getName())) {
				setResolvedType(nameExpression, EolAnyType.Instance);
			} else {

				markers.add(new ModuleMarker(nameExpression, "Undefined variable or type " + nameExpression.getName(),
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
		EolType type = getResolvedType(newInstanceExpression.getTypeExpression());

		// For Tuples, handle EqualsOperatorExpression parameters as named property
		// initialisers (e.g. new Tuple(name = "Bob")).
		if (type instanceof EolTupleType) {
			EolTupleType tupleType = (EolTupleType) type;
			for (Expression parameterExpression : newInstanceExpression.getParameterExpressions()) {
				if (parameterExpression instanceof EqualsOperatorExpression) {
					EqualsOperatorExpression eoe = (EqualsOperatorExpression) parameterExpression;
					eoe.getSecondOperand().accept(this);
					if (eoe.getFirstOperand() instanceof NameExpression) {
						String propertyName = ((NameExpression) eoe.getFirstOperand()).getName();
						EolType valueType = getResolvedType(eoe.getSecondOperand());
						if (!(valueType.equals(EolAnyType.Instance))) {
							tupleType.setPropertyType(propertyName, valueType);
						}
						setResolvedType(eoe.getFirstOperand(), valueType);
					}
					setResolvedType(parameterExpression, EolPrimitiveType.Boolean);
				} else {
					parameterExpression.accept(this);
				}
			}
		} else {
			for (Expression parameterExpression : newInstanceExpression.getParameterExpressions()) {
				parameterExpression.accept(this);
			}
		}

		setResolvedType(newInstanceExpression, type);
		
		//Check for abstract type instantiation
		if(type instanceof EolModelElementType) {
			EolModelElementType mType = (EolModelElementType) type;
			IMetaClass metaClass = mType.getMetaClass();
			if (metaClass != null && metaClass.isAbstract()) {
				markers.add(new ModuleMarker(
					newInstanceExpression,
					"Cannot instantiate an abstract type",
					Severity.Error
				));
			}
		}
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
			visit(parameter, true);
		}
		operation.getBody().accept(this);

		if (getReturnFlag(operation) == false && returnTypeExpression != null)
			markers.add(new ModuleMarker(returnTypeExpression,
					"This operation should return " + returnTypeExpression.getName(), Severity.Error));
		context.getFrameStack().leaveLocal(operation);

	}

	@Override
	public void visit(OperationCallExpression operationCallExpression) {
		List<IStaticOperation> resolvedOperations = new ArrayList<>();
		resolvedOperations.addAll(localOperations);
		resolvedOperations.addAll(importedOperations);
		resolvedOperations.addAll(builtinOperations);
		Expression targetExpression = operationCallExpression.getTargetExpression();
		List<Expression> parameterExpressions = operationCallExpression.getParameterExpressions();
		NameExpression nameExpression = operationCallExpression.getNameExpression();
		EolType contextType = EolNoType.Instance;

		if (targetExpression != null) {
			targetExpression.accept(this);
			operationCallExpression.setContextless(false);
			contextType = getResolvedType(targetExpression);
			if (contextType.getClazz() != null) {
				for(Method m : contextType.getClazz().getMethods()) {
					resolvedOperations.add(methodToSimpleOperation(m, contextType));
				}
			}
		} else
			operationCallExpression.setContextless(true);
		
		for (Expression parameterExpression : parameterExpressions) {
			parameterExpression.accept(this);
		}
		boolean unknownModelElementTarget = isUnknownModelElementType(contextType);

		// Name check
		List<IStaticOperation> temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			if (nameExpression.getName().equals(op.getName())) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			if (contextType.equals(EolAnyType.Instance) || unknownModelElementTarget) {
				setResolvedType(operationCallExpression, EolAnyType.Instance);
				return;
			}
			// Unresolved native type — can't verify operations via reflection
			if (contextType instanceof EolNativeType && contextType.getClazz() == null) {
				setResolvedType(operationCallExpression, EolAnyType.Instance);
				return;
			}
			markers.add(new ModuleMarker(nameExpression, "Undefined operation " + nameExpression.getName(), Severity.Error));
			return;
		}

		// Context check
		temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			EolType opContextType = op.getContextType();
			if (contextType.isAssignableTo(opContextType) || opContextType.isAssignableTo(contextType)) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			if (unknownModelElementTarget) {
				setResolvedType(operationCallExpression, EolAnyType.Instance);
				return;
			}
			markers.add(new ModuleMarker(nameExpression,
					nameExpression.getName() + " can not be invoked on " + getResolvedType(targetExpression),
					Severity.Error));
			return;
		}

//		Number of parameters check
		temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			List<EolType> reqParams = op.getParameterTypes();
			if (op.isVarArgs()) {
				// For varargs, actual args must be >= fixed params (all declared params minus the varargs one)
				if (parameterExpressions.size() >= reqParams.size() - 1) {
					temp.add(op);
				}
			} else if (reqParams.size() == parameterExpressions.size()) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			if (unknownModelElementTarget) {
				setResolvedType(operationCallExpression, EolAnyType.Instance);
				return;
			}
			markers.add(new ModuleMarker(nameExpression, "Parameter number mismatch", Severity.Error));
			return;
		}

//		Parameter type checks
		List<EolType> provParamTypes = parameterExpressions.stream().map(e -> getResolvedType(e))
				.collect(Collectors.toList());
		temp = new ArrayList<IStaticOperation>();
		for (IStaticOperation op : resolvedOperations) {
			List<EolType> reqParamTypes = op.getParameterTypes();
			boolean compatible = true;
			if (op.isVarArgs() && !reqParamTypes.isEmpty()) {
				int fixedCount = reqParamTypes.size() - 1;
				EolType varArgType = reqParamTypes.get(fixedCount);
				// Check fixed parameters
				for (int i = 0; i < fixedCount && i < provParamTypes.size(); i++) {
					EolType provParamType = provParamTypes.get(i);
					EolType reqParamType = reqParamTypes.get(i);
					if (!provParamType.isAssignableTo(reqParamType) && !reqParamType.isAssignableTo(provParamType)) {
						compatible = false;
						break;
					}
				}
				// Check varargs parameters against the component type
				if (compatible) {
					for (int i = fixedCount; i < provParamTypes.size(); i++) {
						EolType provParamType = provParamTypes.get(i);
						if (!provParamType.isAssignableTo(varArgType) && !varArgType.isAssignableTo(provParamType)) {
							compatible = false;
							break;
						}
					}
				}
			} else {
				int index = 0;
				for (EolType reqParamType : reqParamTypes) {
					EolType provParamType = provParamTypes.get(index);
					index++;
					if (!provParamType.isAssignableTo(reqParamType) && !reqParamType.isAssignableTo(provParamType)) {
						compatible = false;
						break;
					}
				}
			}
			if (compatible) {
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		if (resolvedOperations.size() == 0) {
			if (unknownModelElementTarget) {
				setResolvedType(operationCallExpression, EolAnyType.Instance);
				return;
			}
			markers.add(new ModuleMarker(nameExpression,
					"Parameters type mismatch for operation " + nameExpression.getName(), Severity.Error));
			return;
		}
		
		//Select the operations with the most specific context type
		EolType mostSpecificContextType = EolAnyType.Instance;
		temp = new ArrayList<IStaticOperation>();
		for(IStaticOperation op : resolvedOperations) {
			EolType opContextType = op.getContextType();
			
			if(opContextType.equals(EolAnyType.Instance)) {
				if(mostSpecificContextType.equals(EolAnyType.Instance)) {
					temp.add(op);
				}
				continue;
			}

			if(mostSpecificContextType.equals(opContextType) || mostSpecificContextType.isSiblingOf(opContextType)) {
				temp.add(op);
			}
			else if(opContextType.isAssignableTo(mostSpecificContextType)) {
				mostSpecificContextType = opContextType;
				temp.clear();
				temp.add(op);
			}
		}
		resolvedOperations = temp;
		
		if (resolvedOperations.size() == 1) {
			markers.addAll(resolvedOperations.get(0).getExtraDiagnostics(nameExpression, contextType, provParamTypes));
		}

		// Process resolved operations
		Set<EolType> returnTypes = new HashSet<EolType>();
		for (IStaticOperation op : resolvedOperations) {
			returnTypes.add(op.getReturnType(contextType, provParamTypes));
		}
		if (returnTypes.size() == 1) {
			setResolvedType(operationCallExpression, (EolType) returnTypes.toArray()[0]);
		} else {
			setResolvedType(operationCallExpression, new EolUnionType(returnTypes));
		}

		// Check for warning related to context subtypes
		Set<EolType> resolvedOperationContextTypes = new HashSet<EolType>();
		for (IStaticOperation op : resolvedOperations) {
			resolvedOperationContextTypes.add(op.getContextType());
		}
		
		List<EolType> missingTypes = checkMissingTypes(contextType, resolvedOperationContextTypes);
		for(EolType t : missingTypes) {
			markers.add(new ModuleMarker(operationCallExpression,
			"Operation " + nameExpression.getName() + " is undefined for type " + t,
			Severity.Warning));
		}
		
		// Check for warning related to parameter subtypes
		for(int i = 0; i < parameterExpressions.size(); i++) {
			Expression paramExpr = parameterExpressions.get(i);
			EolType paramType = getResolvedType(paramExpr);
			Set<EolType> resolvedOperationParamTypes = new HashSet<EolType>();
			for(IStaticOperation op : resolvedOperations) {
				List<EolType> opParamTypes = op.getParameterTypes();
				// For varargs operations, clamp index to the last declared parameter (the varargs component type)
				int paramIndex = (op.isVarArgs() && i >= opParamTypes.size()) ? opParamTypes.size() - 1 : i;
				resolvedOperationParamTypes.add(opParamTypes.get(paramIndex));
			}
			missingTypes = checkMissingTypes(paramType, resolvedOperationParamTypes);
			for(EolType t : missingTypes) {
				markers.add(new ModuleMarker(paramExpr,
				"Parameter " + (i+1) + " of operation " + nameExpression.getName() + " is undefined for type " + t.getName(),
				Severity.Warning));
			}
		}
	}

	private boolean isUnknownModelElementType(EolType type) {
		if (type instanceof EolTypeLiteral) {
			type = ((EolTypeLiteral) type).getWrappedType();
		}
		if (!(type instanceof EolModelElementType)) {
			return false;
		}
		IMetaClass metaClass = ((EolModelElementType) type).getMetaClass();
		return metaClass != null
				&& metaClass.getMetamodel() instanceof org.eclipse.epsilon.eol.m3.UnknownMetamodel;
	}
	
	private List<EolType> checkMissingTypes(EolType callExpressionType, Set<EolType> operationTypes){
		List<EolType> missingTypes = new ArrayList<EolType>();
		if (operationTypes.contains(EolAnyType.Instance) || callExpressionType.equals(EolAnyType.Instance)) {
			return missingTypes;
		}

		// Contained types are not taken into account for operation context types.
		if(callExpressionType instanceof EolCollectionType) {
			callExpressionType = new EolCollectionType(callExpressionType.getName());
		}
		Set<EolType> temp = new HashSet<EolType>();
		for(EolType opType : operationTypes) {
			if(opType instanceof EolCollectionType) {
				temp.add(new EolCollectionType(opType.getName()));
			}else {
				temp.add(opType);
			}
		}
		operationTypes = temp;
		
		if(callExpressionType instanceof EolTypeLiteral) {
			callExpressionType = new EolTypeLiteral(EolAnyType.Instance);
		}
		
		Stack<EolType> stack = new Stack<EolType>();
		stack.push(callExpressionType);
		outerLoop: while (!stack.isEmpty()) {
			EolType currentNode = stack.pop();
			for (EolType a : currentNode.getAncestors()) {
				if (operationTypes.contains(a)) {
					continue outerLoop;
				}
			}
			if (currentNode.isAbstract()) {
				stack.addAll(currentNode.getChildrenTypes());
			} else {
				missingTypes.add(currentNode);
			}
		}
		return missingTypes;
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
		EolType targetType = getResolvedType(targetExpression);
		boolean typeLiteralTarget = targetType instanceof EolTypeLiteral;
		if (typeLiteralTarget) {
			targetType = ((EolTypeLiteral) targetType).getWrappedType();
			setResolvedType(targetExpression, targetType);
		}
		//Early return if target expression could not be resolved (e.g. out of scope variable)
		if(targetType  == EolAnyType.Instance) {
			setResolvedType(propertyCallExpression, EolAnyType.Instance);
			return;
		}

		// Extended properties
		if (nameExpression.getName().startsWith("~")) {
			setResolvedType(propertyCallExpression, EolAnyType.Instance);
			return;
		}
		
		// Property call on TypeName e.g EPackage.all
		if (targetExpression instanceof NameExpression && ((NameExpression) targetExpression).isTypeName()
				&& getResolvedType(targetExpression) instanceof EolModelElementType) {

			if (nameExpression.getName().equalsIgnoreCase("all")
					|| nameExpression.getName().equalsIgnoreCase("allinstances")
					|| nameExpression.getName().equalsIgnoreCase("getallofkind")
					|| nameExpression.getName().equalsIgnoreCase("getalloftype")) {
				setResolvedType(propertyCallExpression,
						new EolCollectionType("Sequence", getResolvedType(targetExpression)));
			}
			else if (nameExpression.getName().equalsIgnoreCase("createInstance")) {
				setResolvedType(propertyCallExpression, getResolvedType(targetExpression));
			}
			else if (nameExpression.getName().equalsIgnoreCase("isInstantiable")) {
				setResolvedType(propertyCallExpression, EolPrimitiveType.Boolean);
			}
			else {
				setResolvedType(propertyCallExpression, EolAnyType.Instance);
				markers.add(new ModuleMarker(nameExpression, "Property " + nameExpression.getName()
				+ " not found for type " + ((NameExpression)targetExpression).getName(), Severity.Error));
			}
		}
		// Property call on a Java object
		else if (getResolvedType(targetExpression) instanceof EolNativeType) {
			Class<?> javaClass = getResolvedType(targetExpression).getClazz();
			// Unresolved native type — can't verify properties via reflection
			if (javaClass == null) {
				setResolvedType(propertyCallExpression, EolAnyType.Instance);
				return;
			}
			//.x
			try {
				Field f = javaClass.getDeclaredField(nameExpression.getName());
				setResolvedType(propertyCallExpression, new EolNativeType(f.getClass()));
				return;
			} catch (Exception e) {}
			
			String camelCaseName = nameExpression.getName().substring(0, 1).toUpperCase() + nameExpression.getName().substring(1);
			//.getX()
			try {
				Method m = javaClass.getMethod("get" + camelCaseName);
				setResolvedType(propertyCallExpression, new EolNativeType(m.getReturnType()));
				return;
			} catch (Exception e) {}
			
			//.isX()
			try {
				Method m = javaClass.getMethod("is" + camelCaseName);
				setResolvedType(propertyCallExpression, new EolNativeType(m.getReturnType()));
				return;
			} catch (Exception e) {}

			setResolvedType(propertyCallExpression, EolAnyType.Instance);
		}
		// Regular properties
		else {
			EolType type = getResolvedType(targetExpression);

			// Property call on a Tuple
			if (type instanceof EolTupleType) {
				EolTupleType tupleType = (EolTupleType) type;
				String propertyName = nameExpression.getName();
				if (tupleType.hasProperty(propertyName)) {
					setResolvedType(propertyCallExpression, tupleType.getPropertyType(propertyName));
				} else {
					setResolvedType(propertyCallExpression, EolAnyType.Instance);
					// Suppress error when used as target of isDefined/isUndefined/ifDefined/ifUndefined,
					// mirroring the runtime exception-swallowing behaviour in OperationCallExpression.execute()
					ModuleElement parent = propertyCallExpression.getParent();
					if (parent instanceof OperationCallExpression) {
						String opName = ((OperationCallExpression) parent).getNameExpression().getName();
						if ("isDefined".equals(opName) || "isUndefined".equals(opName)
								|| "ifDefined".equals(opName) || "ifUndefined".equals(opName)) {
							// no error
						} else {
							markers.add(new ModuleMarker(nameExpression, "Property '" + propertyName
									+ "' not found", Severity.Error));
						}
					} else {
						markers.add(new ModuleMarker(nameExpression, "Property '" + propertyName
								+ "' not found", Severity.Error));
					}
				}
			}
			else {
				boolean many = false;
				IMetaClass metaClass = null;
				if (type instanceof EolModelElementType && ((EolModelElementType) type).getMetaClass() != null) {
					metaClass = ((EolModelElementType) type).getMetaClass();
				} else if (type instanceof EolCollectionType
						&& ((EolCollectionType) type).getContentType() instanceof EolModelElementType) {
					metaClass = ((EolModelElementType) ((EolCollectionType) type).getContentType()).getMetaClass();
					many = true;
				}

				if (metaClass != null) {
					IProperty property = metaClass.getProperty(nameExpression.getName());
					if (property != null) {
						setResolvedType(propertyCallExpression, toStaticAnalyserType(property.getType()));
						if (many) {
							setResolvedType(propertyCallExpression,
									new EolCollectionType("Sequence", getResolvedType(propertyCallExpression)));
						}

					} else {
						markers.add(new ModuleMarker(nameExpression, "Property " + nameExpression.getName()
								+ " not found in type " + metaClass.getName(), Severity.Error));
					}
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
			EolType requiredReturnType = expectedReturnType(returnStatement);

			if (!providedReturnType.isAssignableTo(requiredReturnType)) {
				if (requiredReturnType.isAssignableTo(providedReturnType))
					markers.add(new ModuleMarker(returnedExpression, "Return type might be " + providedReturnType
							+ " instead of " + requiredReturnType, Severity.Warning));
				else
					markers.add(new ModuleMarker(returnedExpression, "Return type should be " + requiredReturnType
							+ " instead of " + providedReturnType, Severity.Error));

			}
		}
	}
	
	public EolType expectedReturnType(ReturnStatement returnStatement) {
		ModuleElement parent = returnStatement.getParent();
		while (!(parent instanceof Operation) && parent != null) {
			parent = parent.getParent();
		}
		if (parent instanceof Operation) {
			setReturnFlag(((Operation) parent), true);
			return (EolType) parent.getData().get("returnType");
		}
		else {
			return EolAnyType.Instance;
		}
	}

	@Override
	public void visit(SimpleAnnotation simpleAnnotation) {
	}

	@Override
	public void visit(StatementBlock statementBlock) {

		boolean topLevelScope = module != null && statementBlock == module.getMain();

		// Block-wide snapshot: captures the scope as seen on entry (e.g. for
		// cursor positions that are inside the block but before the first
		// statement, or between statements).
		recordVisibleVariables(statementBlock, topLevelScope);

		Region blockRegion = statementBlock.getRegion();
		Position blockEnd = blockRegion != null ? blockRegion.getEnd() : null;

		for (Statement statement : statementBlock.getStatements()) {
			// Snapshot for positions INSIDE this statement's own source
			// region: captured BEFORE the statement is visited, so any
			// variable it introduces is not yet visible.
			recordVisibleVariables(statement, topLevelScope);

			statement.accept(this);

			// "Tail" snapshot covering positions strictly AFTER this
			// statement (up to the end of the enclosing block). By the time
			// this runs the statement has been visited, so any variables it
			// declared are now in scope. A later tail snapshot for the next
			// statement will shadow this one via the smallest-region rule.
			if (blockEnd != null && statement.getRegion() != null
					&& statement.getRegion().getEnd() != null) {
				recordVisibleVariables(new Region(statement.getRegion().getEnd(), blockEnd), topLevelScope);
			}
		}

	}

	@Override
	public void visit(StringLiteral stringLiteral) {
		setResolvedType(stringLiteral, EolPrimitiveType.String);
	}

	@Override
	public void visit(SwitchStatement switchStatement) {
		FrameStack frameStack = context.getFrameStack();

		Expression conditionExpression = switchStatement.getConditionExpression();
		if (conditionExpression != null) {
			conditionExpression.accept(this);
		}

		for (Case c : switchStatement.getCases()) {
			frameStack.enterLocal(FrameType.UNPROTECTED, c);
			c.accept(this);
			frameStack.leaveLocal(c);
		}

		Case defaultCase = switchStatement.getDefault();
		if (defaultCase != null) {
			frameStack.enterLocal(FrameType.UNPROTECTED, defaultCase);
			defaultCase.accept(this);
			frameStack.leaveLocal(defaultCase);
		}
	}

	@Override
	public void visit(TernaryExpression ternaryExpression) {
		Expression condition = ternaryExpression.getFirstOperand();
		condition.accept(this);
		if (!getResolvedType(condition).equals(EolPrimitiveType.Boolean)) {
			markers.add(new ModuleMarker(ternaryExpression, "The first operand of a ternary expression must be a boolean expression",
					Severity.Error));
		}
		
		Expression secondOperand = ternaryExpression.getSecondOperand();
		secondOperand.accept(this);
		Expression thirdOperand = ternaryExpression.getThirdOperand();
		thirdOperand.accept(this);
		setResolvedType(ternaryExpression, new EolUnionType(getResolvedType(secondOperand), getResolvedType(thirdOperand)));
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

		if (type instanceof EolAnyType) {
			setResolvedType(typeExpression, type);
		}

		if (type instanceof EolPrimitiveType) {
			setResolvedType(typeExpression, type);
		}

		if (type instanceof EolCollectionType) {
			setResolvedType(typeExpression, type);
			if (typeExpression.getParameterTypeExpressions().size() == 1) {
				((EolCollectionType) type)
						.setContentType(getResolvedType(typeExpression.getParameterTypeExpressions().get(0)));
				setResolvedType(typeExpression, type);
			} else if (typeExpression.getParameterTypeExpressions().size() > 1) {
				markers.add(new ModuleMarker(typeExpression, "Collection types can have at most one content type",
						Severity.Error));
			}
		}

		if (type instanceof EolMapType) {
			setResolvedType(typeExpression, type);
			if (typeExpression.getParameterTypeExpressions().size() == 2) {
				((EolMapType) type).setKeyType(getResolvedType(typeExpression.getParameterTypeExpressions().get(0)));
				((EolMapType) type).setValueType(getResolvedType(typeExpression.getParameterTypeExpressions().get(1)));
			} else if (typeExpression.getParameterTypeExpressions().size() > 0) {
				markers.add(new ModuleMarker(typeExpression, "Maps need two types: key-type and value-type",
						Severity.Error));
			}
		}

		if (type instanceof EolTupleType) {
			setResolvedType(typeExpression, type);
		}

		if (type == null) {
			// Handle Native type
			if ("Native".equals(typeExpression.getName())) {
				StringLiteral nativeTypeLiteral = typeExpression.getNativeType();
				if (nativeTypeLiteral != null) {
					String className = nativeTypeLiteral.getValue();
					try {
						Class<?> javaClass = Class.forName(className, true, getClass().getClassLoader());
						type = new EolNativeType(javaClass);
					} catch (ClassNotFoundException e) {
						type = new EolNativeType(className);
						markers.add(new ModuleMarker(nativeTypeLiteral,
								"Class " + className + " is not on the classpath",
								Severity.Warning));
					}
				} else {
					type = new EolNativeType(Object.class);
				}
				setResolvedType(typeExpression, type);
				return;
			}
			
			// TODO: Remove duplication between this and NameExpression
			EolModelElementType modelElementType = getModelElementType(typeExpression.getName(), typeExpression);
			if (modelElementType != null) {
				type = modelElementType;
				if (modelElementType.getMetaClass() == null && !context.getModelDeclarations().isEmpty()) {
					markers.add(new ModuleMarker(typeExpression, "Unknown type " + typeExpression.getName(),
							Severity.Error));
				}
			} else if (modelHasNoMetamodel(typeExpression.getName())) {
				type = EolAnyType.Instance;
			} else {
				markers.add(new ModuleMarker(typeExpression, "Undefined variable or type " + typeExpression.getName(),
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
			markers.add(new ModuleMarker(variableDeclaration,
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
			markers.add(new ModuleMarker(conditionExpression, "Condition must be a Boolean", Severity.Error));
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
		
		for (Parameter p : operation.getFormalParameters()) {
			visit(p, false);
		}
		
		operation.getData().put("contextType", contextType);
		operation.getData().put("returnType", returnType);
	}

	public void preValidate(IEolModule module) {

		for (Import import_ : module.getImports()) {
			import_.accept(this);
		}

		for (ModelDeclaration modelDeclaration : module.getDeclaredModelDeclarations()) {
			modelDeclaration.accept(this);
		}

		if (module.getModelDeclarations().isEmpty()) {
			useDefaultUnknownModel();
		}

		if (builtinOperations.isEmpty()) {
			// Parse builtin operations
			List<OperationContributor> operationContributors = context.operationContributorRegistry.stream()
					.collect(Collectors.toList());
			for (OperationContributor oc : operationContributors) {
				EolType contextType = oc.contributesToType();

				for (Method m : oc.getClass().getDeclaredMethods()) {
					if(!m.isSynthetic()) {
						builtinOperations.add(methodToSimpleOperation(m, contextType));
					}
				}
			}
		}

		module.getDeclaredOperations().forEach(o -> operationPreVisitor(o));
		module.getDeclaredOperations().forEach(o -> localOperations.add(new SimpleOperation(o)));
		operationRegistry.put(module.getUri(), localOperations);		
	}

	private void useDefaultUnknownModel() {
		if (context.getModelDeclarations().containsKey(DEFAULT_MODEL_NAME)) return;

		IModel model = new UnknownModel();
		model.setName(DEFAULT_MODEL_NAME);

		ModelDeclaration modelDeclaration = new ModelDeclaration();
		modelDeclaration.setNameExpression(new NameExpression(DEFAULT_MODEL_NAME));
		modelDeclaration.setDriverNameExpression(new NameExpression(UNKNOWN_MODEL_DRIVER));
		modelDeclaration.setModel(model);
		modelDeclaration.setMetamodel(model.getMetamodel(new StringProperties(), context.getRelativePathResolver()));

		context.getRepository().addModel(model);
		context.getModelDeclarations().put(DEFAULT_MODEL_NAME, modelDeclaration);
	}
	
	public SimpleOperation methodToSimpleOperation(Method m, EolType contextType) {
		List<EolType> operationParameterTypes = new ArrayList<EolType>();
		List<String> operationParameterNames = new ArrayList<String>();
		Type[] javaParameterTypes = m.getGenericParameterTypes();
		java.lang.reflect.Parameter[] javaParameters = m.getParameters();
		boolean isVarArgs = m.isVarArgs();
		for (int i = 0; i < javaParameterTypes.length; i++) {
			Type javaParameterType = javaParameterTypes[i];
			operationParameterNames.add(javaParameters.length > i ? javaParameters[i].getName() : "arg" + i);
			if (isVarArgs && i == javaParameterTypes.length - 1) {
				// For varargs, store the component type of the array parameter
				if (javaParameterType instanceof Class<?> && ((Class<?>) javaParameterType).isArray()) {
					operationParameterTypes.add(javaClassToEolType(((Class<?>) javaParameterType).getComponentType()));
				} else {
					operationParameterTypes.add(javaTypeToEolType(javaParameterType));
				}
			} else {
				operationParameterTypes.add(javaTypeToEolType(javaParameterType));
			}
		}
		EolType returnType = javaTypeToEolType(m.getGenericReturnType());
		Optional<MethodTypeCalculator> mtc = Optional.ofNullable(m.getAnnotation(MethodTypeCalculator.class));
		Optional<MethodDiagnosticsCalculator> mdc = Optional
				.ofNullable(m.getAnnotation(MethodDiagnosticsCalculator.class));
		return new SimpleOperation(m.getName(), contextType, returnType, operationParameterTypes, operationParameterNames,
				isVarArgs, mtc, mdc, m);
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
				return javaClassToEolType((Class<?>) rawType);
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
		} else if (javaClass == Integer.class || javaClass == int.class || javaClass == Long.class
				|| javaClass == long.class) {
			return EolPrimitiveType.Integer;
		} else if (javaClass == Double.class || javaClass == double.class || javaClass == Float.class
				|| javaClass == float.class) {
			return EolPrimitiveType.Real;
		} else if (javaClass == boolean.class || javaClass == Boolean.class) {
			return EolPrimitiveType.Boolean;
		} else if (javaClass == java.util.Collection.class) {
			return EolCollectionType.Collection;
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
		} else if (javaClass == org.eclipse.epsilon.eol.types.EolType.class) {
			return new EolTypeLiteral(EolAnyType.Instance);
		} else {
			return new EolNativeType(javaClass);
		}
	}

	public void mainValidate() {

		if (module.getMain() != null)
			module.getMain().accept(this);
		module.getDeclaredOperations().forEach(o -> o.accept(this));
	}

	public void postValidate() {
		context.getFrameStack().dispose();
	}

	/**
	 * Records the variables currently visible at the given AST element. The
	 * snapshot is keyed by the element's source region and can later be queried
	 * via {@link #getCompletions(IEolModule, Position)}.
	 *
	 * <p>"Visible" here matches EOL's scoping rules: local variables shadow
	 * globals, and a protected local frame hides lower local frames while global
	 * frames remain visible.</p>
	 */
	protected void recordVisibleVariables(ModuleElement element) {
		if (element != null) {
			recordVisibleVariables(element.getRegion(), false);
		}
	}

	protected void recordVisibleVariables(ModuleElement element, boolean topLevelScope) {
		if (element != null) {
			recordVisibleVariables(element.getRegion(), topLevelScope);
		}
	}

	protected void recordVisibleVariables(Region region) {
		recordVisibleVariables(region, false);
	}

	protected void recordVisibleVariables(Region region, boolean topLevelScope) {
		if (region == null || region.getStart() == null || region.getEnd() == null) {
			return;
		}
		visibleVariablesRegistry.add(new VisibleVariablesSnapshot(region, captureVisibleVariables(), topLevelScope));
	}

	protected Map<String, Variable> captureVisibleVariables() {
		Map<String, Variable> visible = new LinkedHashMap<String, Variable>();
		captureVisibleVariables(context.getFrameStack().getLocalFrames(), visible);
		captureVisibleVariables(context.getFrameStack().getGlobalFrames(), visible);
		return visible;
	}

	private void captureVisibleVariables(List<SingleFrame> frames, Map<String, Variable> visible) {
		for (SingleFrame frame : frames) {
			for (Map.Entry<String, Variable> entry : frame.getAll().entrySet()) {
				visible.putIfAbsent(entry.getKey(), entry.getValue());
			}
			if (frame.isProtected()) {
				break;
			}
		}
	}

	private static boolean regionContains(Region region, Position position) {
		Position start = region.getStart();
		Position end = region.getEnd();
		if (start == null || end == null) {
			return false;
		}
		// start <= position <= end (inclusive on both ends)
		return !position.isBefore(start) && !end.isBefore(position);
	}

	private static boolean regionIsStrictlyInside(Region inner, Region outer) {
		Position iStart = inner.getStart();
		Position iEnd = inner.getEnd();
		Position oStart = outer.getStart();
		Position oEnd = outer.getEnd();
		if (iStart == null || iEnd == null || oStart == null || oEnd == null) {
			return false;
		}
		boolean startsInside = !iStart.isBefore(oStart);
		boolean endsInside = !oEnd.isBefore(iEnd);
		boolean strictlySmaller = iStart.isAfter(oStart) || oEnd.isAfter(iEnd);
		return startsInside && endsInside && strictlySmaller;
	}

	private static boolean regionEndsBeforeOrAtOnSameLine(Region region, Position position) {
		Position end = region.getEnd();
		if (end == null || position == null || end.getLine() != position.getLine()) {
			return false;
		}
		return regionEndsBeforeOrAt(region, position);
	}

	private static boolean regionEndsBeforeOrAt(Region region, Position position) {
		Position end = region.getEnd();
		if (end == null || position == null) {
			return false;
		}
		return !position.isBefore(end);
	}

	private static boolean isBetterPrecedingSnapshot(VisibleVariablesSnapshot candidate,
			VisibleVariablesSnapshot best) {
		if (best == null) {
			return true;
		}
		Position candidateEnd = candidate.region.getEnd();
		Position bestEnd = best.region.getEnd();
		if (candidateEnd.isAfter(bestEnd) && !candidateEnd.equals(bestEnd)) {
			return true;
		}
		return candidateEnd.equals(bestEnd) && regionIsStrictlyInside(candidate.region, best.region);
	}

	public List<EolCompletion> getCompletions(IEolModule module, Position position) {
		if (module == null || position == null) {
			return Collections.emptyList();
		}

		MemberCompletionContext memberCompletion = findMemberCompletion(module, position);
		if (memberCompletion != null) {
			return getMemberCompletions(memberCompletion, position);
		}

		EnumerationLiteralCompletionContext enumerationLiteralCompletion = findEnumerationLiteralCompletion(module,
				position);
		if (enumerationLiteralCompletion != null) {
			return getEnumerationLiteralCompletions(enumerationLiteralCompletion);
		}

		TypeCompletionContext typeCompletion = getTypeCompletionContext(module, position);
		if (typeCompletion.typeExpression) {
			Map<String, EolCompletion> completions = new LinkedHashMap<String, EolCompletion>();
			addTypeCompletions(completions, typeCompletion);
			return sortedCompletions(completions);
		}

		VisibleVariablesSnapshot best = null;
		for (VisibleVariablesSnapshot snapshot : visibleVariablesRegistry) {
			if (!regionContains(snapshot.region, position)) {
				continue;
			}
			if (best == null || regionIsStrictlyInside(snapshot.region, best.region)) {
				best = snapshot;
			}
		}

		if (best == null) {
			for (VisibleVariablesSnapshot snapshot : visibleVariablesRegistry) {
				if (!regionEndsBeforeOrAtOnSameLine(snapshot.region, position)) {
					continue;
				}
				if (isBetterPrecedingSnapshot(snapshot, best)) {
					best = snapshot;
				}
			}
		}

		if (best == null) {
			// Trailing whitespace/comments after main statements are still top-level scope.
			for (VisibleVariablesSnapshot snapshot : visibleVariablesRegistry) {
				if (!snapshot.topLevelScope || !regionEndsBeforeOrAt(snapshot.region, position)) {
					continue;
				}
				if (isBetterPrecedingSnapshot(snapshot, best)) {
					best = snapshot;
				}
			}
		}

		if (best == null) {
			Map<String, EolCompletion> completions = new LinkedHashMap<String, EolCompletion>();
			addTypeCompletions(completions, typeCompletion);
			return sortedCompletions(completions);
		}

		Map<String, EolCompletion> completions = new LinkedHashMap<String, EolCompletion>();
		for (Map.Entry<String, Variable> entry : best.variables.entrySet()) {
			String name = entry.getKey();
			Variable variable = entry.getValue();
			EolCompletionKind kind = isSpecialVariableName(name)
					? EolCompletionKind.SPECIAL_VARIABLE
					: EolCompletionKind.VARIABLE;
			completions.putIfAbsent(name, new EolCompletion(name, kind, variable.getType()));
		}

		addTypeCompletions(completions, typeCompletion);

		return sortedCompletions(completions);
	}

	private List<EolCompletion> sortedCompletions(Map<String, EolCompletion> completions) {
		List<EolCompletion> result = new ArrayList<EolCompletion>(completions.values());
		Collections.sort(result, Comparator.comparing(EolCompletion::getName));
		return result;
	}

	private MemberCompletionContext findMemberCompletion(ModuleElement element, Position position) {
		return findMemberCompletion(element, position, null);
	}

	private MemberCompletionContext findMemberCompletion(ModuleElement element, Position position,
			MemberCompletionContext best) {
		if (element == null) {
			return best;
		}

		MemberCompletionContext candidate = toMemberCompletionContext(element, position);
		if (candidate != null && isBetterMemberCompletion(candidate, best)) {
			best = candidate;
		}

		for (ModuleElement child : element.getChildren()) {
			best = findMemberCompletion(child, position, best);
		}
		return best;
	}

	private MemberCompletionContext toMemberCompletionContext(ModuleElement element, Position position) {
		if (element instanceof OperationCallExpression) {
			OperationCallExpression operationCallExpression = (OperationCallExpression) element;
			Expression targetExpression = operationCallExpression.getTargetExpression();
			NameExpression nameExpression = operationCallExpression.getNameExpression();
			if (targetExpression == null || !positionMatchesNameRegion(nameExpression, position)) {
				return null;
			}
			return new MemberCompletionContext(nameExpression, getResolvedType(targetExpression), true, false);
		}

		if (element instanceof PropertyCallExpression) {
			PropertyCallExpression propertyCallExpression = (PropertyCallExpression) element;
			Expression targetExpression = propertyCallExpression.getTargetExpression();
			NameExpression nameExpression = propertyCallExpression.getNameExpression();
			if (targetExpression == null || !positionMatchesNameRegion(nameExpression, position)) {
				return null;
			}

			EolType targetType = getResolvedType(targetExpression);
			boolean typeLiteralTarget = targetType instanceof EolTypeLiteral
					|| targetExpression instanceof NameExpression && ((NameExpression) targetExpression).isTypeName();
			if (targetType instanceof EolTypeLiteral) {
				targetType = ((EolTypeLiteral) targetType).getWrappedType();
			}
			return new MemberCompletionContext(nameExpression, targetType, false, typeLiteralTarget);
		}

		return null;
	}

	private boolean isBetterMemberCompletion(MemberCompletionContext candidate, MemberCompletionContext best) {
		return best == null || regionIsStrictlyInside(candidate.getNameRegion(), best.getNameRegion());
	}

	private EnumerationLiteralCompletionContext findEnumerationLiteralCompletion(ModuleElement element,
			Position position) {
		return findEnumerationLiteralCompletion(element, position, null);
	}

	private EnumerationLiteralCompletionContext findEnumerationLiteralCompletion(ModuleElement element,
			Position position, EnumerationLiteralCompletionContext best) {
		if (element == null) {
			return best;
		}

		EnumerationLiteralCompletionContext candidate = toEnumerationLiteralCompletionContext(element, position);
		if (candidate != null && (best == null || regionIsStrictlyInside(candidate.getRegion(), best.getRegion()))) {
			best = candidate;
		}

		for (ModuleElement child : element.getChildren()) {
			best = findEnumerationLiteralCompletion(child, position, best);
		}
		return best;
	}

	private EnumerationLiteralCompletionContext toEnumerationLiteralCompletionContext(ModuleElement element,
			Position position) {
		if (!(element instanceof EnumerationLiteralExpression)
				|| !positionMatchesNameRegion(element.getRegion(), position)) {
			return null;
		}

		EnumerationLiteralExpression expression = (EnumerationLiteralExpression) element;
		String textBeforeCursor = getCompletionPrefix(expression.getEnumerationLiteral(), expression.getRegion(),
				position);
		int hashIndex = textBeforeCursor.indexOf('#');
		if (hashIndex < 0) {
			return null;
		}

		String enumerationName = textBeforeCursor.substring(0, hashIndex);
		if (enumerationName.isEmpty()) {
			return null;
		}

		String prefix = textBeforeCursor.substring(hashIndex + 1);
		return new EnumerationLiteralCompletionContext(expression, enumerationName, prefix);
	}

	private static boolean positionMatchesNameRegion(NameExpression nameExpression, Position position) {
		return nameExpression != null && positionMatchesNameRegion(nameExpression.getRegion(), position);
	}

	private static boolean positionMatchesNameRegion(Region region, Position position) {
		if (region == null || position == null || region.getStart() == null || region.getEnd() == null) {
			return false;
		}
		Position start = region.getStart();
		Position end = region.getEnd();
		if (start.getLine() != position.getLine() || position.getColumn() < start.getColumn()) {
			return false;
		}
		return end.getLine() != position.getLine() || position.getColumn() <= end.getColumn() + 1;
	}

	private NameExpression findNameCompletion(ModuleElement element, Position position) {
		return findNameCompletion(element, position, null);
	}

	private TypeCompletionContext getTypeCompletionContext(ModuleElement element, Position position) {
		TypeExpression typeCompletion = findTypeCompletion(element, position);
		if (typeCompletion != null) {
			return toTypeCompletionContext(getCompletionPrefix(typeCompletion, position), true);
		}

		NameExpression nameCompletion = findNameCompletion(element, position);
		return toTypeCompletionContext(getCompletionPrefix(nameCompletion, position), false);
	}

	private TypeExpression findTypeCompletion(ModuleElement element, Position position) {
		return findTypeCompletion(element, position, null);
	}

	private TypeExpression findTypeCompletion(ModuleElement element, Position position, TypeExpression best) {
		if (element == null) {
			return best;
		}

		if (element instanceof TypeExpression && positionMatchesNameRegion(element.getRegion(), position)) {
			TypeExpression candidate = (TypeExpression) element;
			if (best == null || regionIsStrictlyInside(candidate.getRegion(), best.getRegion())) {
				best = candidate;
			}
		}

		for (ModuleElement child : element.getChildren()) {
			best = findTypeCompletion(child, position, best);
		}
		return best;
	}

	private TypeCompletionContext toTypeCompletionContext(String textBeforeCursor, boolean typeExpression) {
		if (textBeforeCursor == null) {
			return new TypeCompletionContext(null, new String[0], "", typeExpression);
		}

		String text = textBeforeCursor;
		String modelName = null;
		int modelDelimiter = text.indexOf('!');
		if (modelDelimiter >= 0) {
			modelName = text.substring(0, modelDelimiter);
			text = text.substring(modelDelimiter + 1);
		}

		int packageDelimiter = text.lastIndexOf("::");
		if (packageDelimiter < 0) {
			return new TypeCompletionContext(modelName, new String[0], text, typeExpression);
		}

		String packagePrefix = text.substring(0, packageDelimiter);
		String prefix = text.substring(packageDelimiter + 2);
		String[] packageNames = packagePrefix.isEmpty() ? new String[0] : packagePrefix.split("::");
		return new TypeCompletionContext(modelName, packageNames, prefix, typeExpression);
	}

	private NameExpression findNameCompletion(ModuleElement element, Position position, NameExpression best) {
		if (element == null) {
			return best;
		}

		if (element instanceof NameExpression && positionMatchesNameRegion((NameExpression) element, position)) {
			NameExpression candidate = (NameExpression) element;
			if (best == null || regionIsStrictlyInside(candidate.getRegion(), best.getRegion())) {
				best = candidate;
			}
		}

		for (ModuleElement child : element.getChildren()) {
			best = findNameCompletion(child, position, best);
		}
		return best;
	}

	private List<EolCompletion> getMemberCompletions(MemberCompletionContext context, Position position) {
		String prefix = getCompletionPrefix(context, position);
		Map<String, EolCompletion> completions = new LinkedHashMap<String, EolCompletion>();
		if (!context.operationCall) {
			addPropertyCompletions(completions, context.targetType, context.typeLiteralTarget, prefix);
		}
		addOperationCompletions(completions, context.targetType, prefix);
		List<EolCompletion> result = new ArrayList<EolCompletion>(completions.values());
		Collections.sort(result, Comparator.comparing(EolCompletion::getName));
		return result;
	}

	private List<EolCompletion> getEnumerationLiteralCompletions(EnumerationLiteralCompletionContext context) {
		Map<String, EolCompletion> completions = new LinkedHashMap<String, EolCompletion>();
		EolModelElementType enumType = getModelElementType(context.enumerationName, context.expression);
		if (enumType == null || !(enumType.getMetaClass() instanceof IEnum)) {
			return sortedCompletions(completions);
		}

		IEnum enumeration = (IEnum) enumType.getMetaClass();
		for (String literal : enumeration.getLiterals()) {
			if (literal.startsWith(context.prefix)) {
				completions.putIfAbsent(literal,
						new EolCompletion(literal, EolCompletionKind.VARIABLE, enumType, "enum literal"));
			}
		}
		return sortedCompletions(completions);
	}

	private String getCompletionPrefix(MemberCompletionContext context, Position position) {
		return getCompletionPrefix(context.nameExpression, position);
	}

	private String getCompletionPrefix(NameExpression nameExpression, Position position) {
		return getCompletionPrefix(nameExpression != null ? nameExpression.getName() : null,
				nameExpression != null ? nameExpression.getRegion() : null, position);
	}

	private String getCompletionPrefix(TypeExpression typeExpression, Position position) {
		return getCompletionPrefix(typeExpression != null ? typeExpression.getName() : null,
				typeExpression != null ? typeExpression.getRegion() : null, position);
	}

	private String getCompletionPrefix(String typedName, Region region, Position position) {
		if (typedName == null || EolCompletionParseRepairer.PLACEHOLDER.equals(typedName)) {
			return "";
		}
		Position start = region != null ? region.getStart() : null;
		if (start == null || start.getLine() != position.getLine()) {
			return typedName;
		}
		int length = Math.min(typedName.length(), Math.max(0, position.getColumn() - start.getColumn()));
		return typedName.substring(0, length);
	}

	private void addOperationCompletions(Map<String, EolCompletion> completions, EolType contextType, String prefix) {
		if (contextType == null) {
			return;
		}

		List<IStaticOperation> operations = new ArrayList<IStaticOperation>();
		operations.addAll(localOperations);
		operations.addAll(importedOperations);
		operations.addAll(builtinOperations);
		operations.addAll(firstOrderOperationsForCompletion());
		if (contextType.getClazz() != null) {
			for (Method method : contextType.getClazz().getMethods()) {
				operations.add(methodToSimpleOperation(method, contextType));
			}
		}

		for (IStaticOperation operation : operations) {
			if (!operation.getName().startsWith(prefix) || !operationShouldBeSuggested(operation, contextType)) {
				continue;
			}
			EolType returnType = operationReturnTypeForCompletion(operation, contextType);
			String returnTypeName = completionTypeName(returnType);
			String signature = operationSignature(operation, returnTypeName);
			completions.putIfAbsent(signature,
					new EolCompletion(operation.getName(), EolCompletionKind.OPERATION, returnType, returnTypeName, signature));
		}
	}

	private List<IStaticOperation> firstOrderOperationsForCompletion() {
		List<IStaticOperation> operations = new ArrayList<IStaticOperation>();
		for (Map.Entry<String, AbstractOperation> operation : context.operationFactory.getOperations().entrySet()) {
			if (operation.getValue() instanceof FirstOrderOperation) {
				operations.add(new FirstOrderStaticOperation(operation.getKey(), operation.getValue()));
			}
		}
		return operations;
	}

	private EolType operationReturnTypeForCompletion(IStaticOperation operation, EolType contextType) {
		try {
			return operation.getReturnType(contextType, operation.getParameterTypes());
		}
		catch (Exception e) {
			return EolAnyType.Instance;
		}
	}

	private String operationSignature(IStaticOperation operation, String returnTypeName) {
		StringBuilder signature = new StringBuilder(operation.getName());
		signature.append("(");
		List<EolType> parameterTypes = operation.getParameterTypes();
		List<String> parameterNames = operation.getParameterNames();
		for (int i = 0; i < parameterTypes.size(); i++) {
			if (i > 0) {
				signature.append(", ");
			}
			String parameterName = parameterNames != null && i < parameterNames.size() ? parameterNames.get(i) : "arg" + i;
			EolType parameterType = parameterTypes.get(i);
			signature.append(parameterName != null && !parameterName.isEmpty() ? parameterName : "arg" + i);
			signature.append(" : ");
			signature.append(parameterType != null ? parameterType.toString() : EolAnyType.Instance.toString());
			if (operation.isVarArgs() && i == parameterTypes.size() - 1) {
				signature.append("...");
			}
		}
		signature.append(")");
		signature.append(" : ");
		signature.append(returnTypeName);
		return signature.toString();
	}

	private String completionTypeName(EolType type) {
		return type != null ? type.toString() : EolAnyType.Instance.toString();
	}

	private boolean operationShouldBeSuggested(IStaticOperation operation, EolType contextType) {
		if (!operationAppliesToCompletionContext(operation, contextType)) {
			return false;
		}

		if (!(operation instanceof SimpleOperation)) {
			return true;
		}

		Method method = ((SimpleOperation) operation).getMethod();
		if (method == null) {
			return true;
		}

		if (!Modifier.isPublic(method.getModifiers())) {
			return false;
		}

		Class<?> declaringClass = method.getDeclaringClass();
		if (!OperationContributor.class.isAssignableFrom(declaringClass)) {
			return true;
		}

		if (StringUtil.isOneOf(method.getName(), "contributesTo", "contributesToType")) {
			return false;
		}

		return contributorMethodAppliesToContext(declaringClass, contextType);
	}

	private boolean contributorMethodAppliesToContext(Class<?> contributorClass, EolType contextType) {
		if (contextType == null || contextType == EolAnyType.Instance) {
			return true;
		}
		if (contributorClass == org.eclipse.epsilon.eol.execute.operations.contributors.IterableOperationContributor.class) {
			return isIterableCompletionContext(contextType);
		}
		if (contributorClass == org.eclipse.epsilon.eol.execute.operations.contributors.BooleanOperationContributor.class) {
			return typeMatches(contextType, EolPrimitiveType.Boolean);
		}
		if (contributorClass == org.eclipse.epsilon.eol.execute.operations.contributors.compatibility.StringCompatibilityOperationContributor.class) {
			return typeMatches(contextType, EolPrimitiveType.String);
		}
		if (contributorClass == org.eclipse.epsilon.eol.execute.operations.contributors.BasicEUnitOperationContributor.class) {
			return contextType == EolNoType.Instance;
		}
		if (contributorClass == org.eclipse.epsilon.eol.execute.operations.contributors.ModelElementOperationContributor.class) {
			return contextType instanceof EolModelElementType;
		}
		if (contributorClass == org.eclipse.epsilon.eol.execute.operations.contributors.ArrayOperationContributor.class) {
			return isArrayCompletionContext(contextType);
		}
		if (contributorClass == org.eclipse.epsilon.eol.execute.operations.contributors.ScalarOperationContributor.class) {
			return !(contextType instanceof EolCollectionType) && !isIterableCompletionContext(contextType);
		}
		return true;
	}

	private boolean typeMatches(EolType actualType, EolType expectedType) {
		return actualType.isAssignableTo(expectedType) || expectedType.isAssignableTo(actualType);
	}

	private boolean isIterableCompletionContext(EolType contextType) {
		if (contextType instanceof EolCollectionType) {
			return true;
		}
		Class<?> clazz = contextType.getClazz();
		return clazz != null && (clazz.isArray() || Iterable.class.isAssignableFrom(clazz));
	}

	private boolean isArrayCompletionContext(EolType contextType) {
		Class<?> clazz = contextType.getClazz();
		return clazz != null && clazz.isArray();
	}

	private boolean operationAppliesToCompletionContext(IStaticOperation operation, EolType contextType) {
		EolType operationContextType = operation.getContextType();
		if (contextType == null) {
			return false;
		}
		if (contextType == EolAnyType.Instance) {
			return operationContextType == EolAnyType.Instance;
		}
		return contextType.isAssignableTo(operationContextType);
	}

	private void addPropertyCompletions(Map<String, EolCompletion> completions, EolType targetType,
			boolean typeLiteralTarget, String prefix) {
		if (targetType instanceof EolTypeLiteral) {
			targetType = ((EolTypeLiteral) targetType).getWrappedType();
		}
		if (targetType == null || targetType == EolAnyType.Instance) {
			return;
		}

		if (typeLiteralTarget && targetType instanceof EolModelElementType) {
			addPropertyCompletion(completions, "all", new EolCollectionType("Sequence", targetType), prefix);
			addPropertyCompletion(completions, "allInstances", new EolCollectionType("Sequence", targetType), prefix);
			addPropertyCompletion(completions, "getAllOfKind", new EolCollectionType("Sequence", targetType), prefix);
			addPropertyCompletion(completions, "getAllOfType", new EolCollectionType("Sequence", targetType), prefix);
			addPropertyCompletion(completions, "createInstance", targetType, prefix);
			addPropertyCompletion(completions, "isInstantiable", EolPrimitiveType.Boolean, prefix);
			return;
		}

		if (targetType instanceof EolTupleType) {
			for (Map.Entry<String, EolType> property : ((EolTupleType) targetType).getPropertyTypes().entrySet()) {
				addPropertyCompletion(completions, property.getKey(), property.getValue(), prefix);
			}
			return;
		}

		IMetaClass metaClass = null;
		boolean many = false;
		if (targetType instanceof EolModelElementType && ((EolModelElementType) targetType).getMetaClass() != null) {
			metaClass = ((EolModelElementType) targetType).getMetaClass();
		}
		else if (targetType instanceof EolCollectionType
				&& ((EolCollectionType) targetType).getContentType() instanceof EolModelElementType) {
			EolModelElementType contentType = (EolModelElementType) ((EolCollectionType) targetType).getContentType();
			metaClass = contentType.getMetaClass();
			many = true;
		}

		if (metaClass != null) {
			for (IProperty property : metaClass.getAllProperties()) {
				EolType propertyType = toStaticAnalyserType(property.getType());
				if (many) {
					propertyType = new EolCollectionType("Sequence", propertyType);
				}
				addPropertyCompletion(completions, property.getName(), propertyType, prefix);
			}
			return;
		}

		if (targetType instanceof EolNativeType) {
			addNativePropertyCompletions(completions, targetType, prefix);
		}
	}

	private void addNativePropertyCompletions(Map<String, EolCompletion> completions, EolType targetType, String prefix) {
		Class<?> javaClass = targetType.getClazz();
		if (javaClass == null) {
			return;
		}
		for (Field field : javaClass.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			addPropertyCompletion(completions, field.getName(), javaClassToEolType(field.getType()), prefix);
		}
		for (Method method : javaClass.getMethods()) {
			if (Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 0) {
				continue;
			}
			String propertyName = getterPropertyName(method);
			if (propertyName != null) {
				addPropertyCompletion(completions, propertyName, javaClassToEolType(method.getReturnType()), prefix);
			}
		}
	}

	private String getterPropertyName(Method method) {
		String methodName = method.getName();
		if (methodName.startsWith("get") && methodName.length() > 3) {
			return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
		}
		if (methodName.startsWith("is") && methodName.length() > 2) {
			return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
		}
		return null;
	}

	private void addPropertyCompletion(Map<String, EolCompletion> completions, String name, EolType type, String prefix) {
		if (name.startsWith(prefix)) {
			completions.putIfAbsent(name, new EolCompletion(name, EolCompletionKind.PROPERTY, type));
		}
	}

	private void addTypeCompletions(Map<String, EolCompletion> completions, TypeCompletionContext typeCompletion) {
		if (typeCompletion.modelName == null && !typeCompletion.hasPackageQualifier()) {
			addBuiltinTypeCompletions(completions, typeCompletion.prefix);
		}

		if (typeCompletion.typeExpression && typeCompletion.modelName == null && !typeCompletion.hasPackageQualifier()
				&& !typeCompletion.prefix.isEmpty()) {
			addModelNameCompletions(completions, typeCompletion.prefix);
		}

		for (Map.Entry<String, ModelDeclaration> entry : context.modelDeclarations.entrySet()) {
			if (typeCompletion.modelName != null && !typeCompletion.modelName.equals(entry.getKey())) {
				continue;
			}

			ModelDeclaration modelDeclaration = entry.getValue();
			IMetamodel metamodel = modelDeclaration.getMetamodel();
			if (metamodel == null) {
				continue;
			}

			if (typeCompletion.hasPackageQualifier()) {
				org.eclipse.epsilon.eol.m3.Package pkg = resolvePackage(metamodel, typeCompletion.packageNames);
				if (pkg != null) {
					addPackageTypeCompletions(completions, pkg, typeCompletion.prefix);
				}
			}
			else {
				addTypeCompletions(completions, metamodel.getTypes(), metamodel.getSubPackages(), typeCompletion.prefix);
			}
		}
	}

	private void addBuiltinTypeCompletions(Map<String, EolCompletion> completions, String prefix) {
		for (String name : BUILTIN_TYPE_COMPLETION_NAMES) {
			if (!name.startsWith(prefix)) {
				continue;
			}
			EolType type = "Native".equals(name)
					? new EolNativeType(Object.class)
					: toStaticAnalyserType(TypeExpression.getType(name));
			completions.putIfAbsent(name, new EolCompletion(name, EolCompletionKind.VARIABLE, type, "type"));
		}
	}

	private void addModelNameCompletions(Map<String, EolCompletion> completions, String prefix) {
		for (String modelName : context.modelDeclarations.keySet()) {
			String name = modelName + "!";
			if (name.startsWith(prefix)) {
				completions.putIfAbsent(name, new EolCompletion(name, EolCompletionKind.VARIABLE, null, "model"));
			}
		}
	}

	private org.eclipse.epsilon.eol.m3.Package resolvePackage(IMetamodel metamodel, String[] packageNames) {
		org.eclipse.epsilon.eol.m3.Package current = null;
		int index = 0;
		if (metamodel instanceof org.eclipse.epsilon.eol.m3.Package) {
			current = (org.eclipse.epsilon.eol.m3.Package) metamodel;
			if (current.getName() != null && packageNames.length > 0 && current.getName().equals(packageNames[0])) {
				index = 1;
			}
		}

		if (index == packageNames.length) {
			return current;
		}

		List<org.eclipse.epsilon.eol.m3.Package> subPackages = current != null
				? current.getSubPackages()
				: metamodel.getSubPackages();
		for (int i = index; i < packageNames.length; i++) {
			org.eclipse.epsilon.eol.m3.Package next = null;
			for (org.eclipse.epsilon.eol.m3.Package subPackage : subPackages) {
				if (packageNames[i].equals(subPackage.getName())) {
					next = subPackage;
					break;
				}
			}
			if (next == null) {
				return null;
			}
			current = next;
			subPackages = current.getSubPackages();
		}
		return current;
	}

	private void addPackageTypeCompletions(Map<String, EolCompletion> completions,
			org.eclipse.epsilon.eol.m3.Package pkg, String prefix) {
		for (IMetaClass type : pkg.getTypes()) {
			String name = type.getName();
			if (name.startsWith(prefix)) {
				completions.putIfAbsent(name,
						new EolCompletion(name, EolCompletionKind.VARIABLE, new EolModelElementType(type)));
			}
		}
		for (org.eclipse.epsilon.eol.m3.Package subPackage : pkg.getSubPackages()) {
			String name = subPackage.getName();
			if (name.startsWith(prefix)) {
				completions.putIfAbsent(name, new EolCompletion(name, EolCompletionKind.VARIABLE, null, "package"));
			}
		}
	}

	private void addTypeCompletions(Map<String, EolCompletion> completions, List<IMetaClass> types,
			List<org.eclipse.epsilon.eol.m3.Package> subPackages, String prefix) {
		for (IMetaClass type : types) {
			String name = type.getName();
			if (name.startsWith(prefix)) {
				completions.putIfAbsent(name,
						new EolCompletion(name, EolCompletionKind.VARIABLE, new EolModelElementType(type)));
			}
		}
		for (org.eclipse.epsilon.eol.m3.Package subPackage : subPackages) {
			addTypeCompletions(completions, subPackage.getTypes(), subPackage.getSubPackages(), prefix);
		}
	}

	private static boolean isSpecialVariableName(String name) {
		return "self".equals(name) || "loopCount".equals(name) || "hasMore".equals(name);
	}

	@Override
	public List<ModuleMarker> validate(IModule imodule) {
	
		markers = new ArrayList<ModuleMarker>();
		visibleVariablesRegistry = new ArrayList<VisibleVariablesSnapshot>();
		this.module = (IEolModule) imodule;

		preValidate(module);
		mainValidate();
		postValidate();
		return markers;
	}

	@Override
	public String getMarkerType() {
		return ModuleMarker.Severity.Error.name();
	}

	public void createTypeCompatibilityWarning(Expression requiredExpression, Expression providedExpression) {
		markers.add(new ModuleMarker(providedExpression,
				getResolvedType(providedExpression) + " may not be assigned to " + getResolvedType(requiredExpression),
				Severity.Warning));
	}

	public void createTypeCompatibilityError(Expression requiredExpression, Expression providedExpression) {
		markers.add(new ModuleMarker(providedExpression,
				getResolvedType(providedExpression) + " cannot be assigned to " + getResolvedType(requiredExpression),
				Severity.Error));
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
					markers.add(new ModuleMarker(operatorExpression,
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
					markers.add(new ModuleMarker(operatorExpression,
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

	public void setResolvedType(AbstractExecutableModuleElement e, EolType type) {
		e.getData().put("resolvedType", type);
	}

	public EolType getResolvedType(AbstractExecutableModuleElement e) {
		EolType resolvedType = (EolType) e.getData().get("resolvedType");
		if (resolvedType == null) {
			resolvedType = EolAnyType.Instance;
			setResolvedType(e, resolvedType);
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

	public EolStaticAnalysisContext getContext() {
		return context;
	}

	public void setContext(EolStaticAnalysisContext context) {
		this.context = context;
	}

	/**
	 * Returns true if the given modelAndType string refers to a model whose
	 * driver does not expose a metamodel. This is used to suppress type errors
	 * for models where no static type information is available.
	 */
	private boolean modelHasNoMetamodel(String modelAndType) {
		String modelName;
		if (modelAndType.contains("!")) {
			modelName = modelAndType.split("!")[0];
		} else {
			modelName = "";
		}
		IModel model = context.repository.getModelByNameSafe(modelName);
		if (model == null) {
			return false;
		}
		if (modelName.isEmpty()) {
			modelName = model.getName();
		}
		ModelDeclaration md = context.modelDeclarations.get(modelName);
		return md != null && md.getMetamodel() == null;
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
				markers.add(new ModuleMarker(element,
						"Ambiguous type, consider using a concrete model name istead of an alias", Severity.Warning));
			}

		}
		if (modelName == "") {
			modelName = model.getName();
		}

		// When the type is unqualified (no model prefix), iterate through all
		// model declarations and prefer types from non-Unknown metamodels so
		// that the Unknown driver does not shadow legitimate types.
		if (!modelAndType.contains("!")) {
			EolModelElementType unknownFallback = null;
			for (Map.Entry<String, ModelDeclaration> entry : context.modelDeclarations.entrySet()) {
				IMetamodel mm = entry.getValue().getMetamodel();
				if (mm == null) continue;
				IMetaClass mc = mm.getMetaClass(typeName);
				if (mc == null) continue;
				EolModelElementType met = new EolModelElementType(modelAndType, module);
				met.setMetaClass(mc);
				if (mm instanceof org.eclipse.epsilon.eol.m3.UnknownMetamodel) {
					if (unknownFallback == null) {
						unknownFallback = met;
					}
				} else {
					return met;
				}
			}
			return unknownFallback;
		}

		IMetamodel metamodel = context.modelDeclarations.get(modelName).getMetamodel();
		if (metamodel != null) {
			IMetaClass metaclass = metamodel.getMetaClass(typeName);
			if (metaclass == null) {
				return null;
			}
			else {
				EolModelElementType modelElementType = new EolModelElementType(modelAndType, module);
				modelElementType.setMetaClass(metaclass);
				return modelElementType;
			}
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
				if ( ((org.eclipse.epsilon.eol.types.EolCollectionType)type).getContentType() != null) {
					return new EolCollectionType(name, toStaticAnalyserType(((org.eclipse.epsilon.eol.types.EolCollectionType) type).getContentType()));
				}else {
					 return new EolCollectionType(name);
				}
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
