package org.eclipse.epsilon.eol.staticanalyser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.epsilon.common.module.AbstractModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.common.module.ModuleMarker.Severity;
import org.eclipse.epsilon.eol.dom.ModelDeclaration;
import org.eclipse.epsilon.eol.staticanalyser.execute.context.FrameStack;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.operations.contributors.OperationContributorRegistry;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.models.ModelRepository;

public class EolStaticAnalysisContext {

	protected List<ModuleMarker> markers = new ArrayList<>();
	protected IEolContext runtimeContext = null;
	protected FrameStack frameStack = new FrameStack();
	protected IModelFactory modelFactory;
	protected IRelativePathResolver relativePathResolver;
	protected Map<String, ModelDeclaration> modelDeclarations = new HashMap<>();
	protected ModelRepository repository = new ModelRepository();
	protected OperationContributorRegistry  operationContributorRegistry = new OperationContributorRegistry();
	
	
	public Map<String, ModelDeclaration> getModelDeclarations() {
		return modelDeclarations;
	}
	
	public ModelRepository getRepository() {
		return repository;
	}

	public void setRepository(ModelRepository repository) {
		this.repository = repository;
	}

	public List<ModuleMarker> getMarkers() {
		return markers;
	}
	
	public void setRuntimeContext(IEolContext context) {
		this.runtimeContext = context;
	}

	public void addWarningMarker(AbstractModuleElement element, String message) {
		markers.add(new ModuleMarker(element, message, Severity.Warning));
	}

	public void addErrorMarker(AbstractModuleElement element, String message) {
		markers.add(new ModuleMarker(element, message, Severity.Error));
	}
	
	public FrameStack getFrameStack() {
		return frameStack;
	}
	
	public IModelFactory getModelFactory() {
		return modelFactory;
	}
	
	public void setModelFactory(IModelFactory modelFactory) {
		this.modelFactory = modelFactory;
	}
	
	public IRelativePathResolver getRelativePathResolver() {
		return relativePathResolver;
	}
	
	public void setRelativePathResolver(IRelativePathResolver relativePathResolver) {
		this.relativePathResolver = relativePathResolver;
	}
}