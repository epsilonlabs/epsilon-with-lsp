package org.eclipse.epsilon.egl.analyse;

import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.egl.IEglModule;
import org.eclipse.epsilon.egl.output.IOutputBuffer;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.analyse.EolStaticAnalyser;
import org.eclipse.epsilon.eol.analyse.IModelFactory;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.types.EolNativeType;

public class EglStaticAnalyser extends EolStaticAnalyser {
	
	public EglStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}
	
	public EglStaticAnalyser() {
	}
	
	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof IEglModule))
			return Collections.emptyList();

		this.module = (IEolModule) imodule;
		IEglModule eglModule = (IEglModule) imodule;
		

		super.preValidate(eglModule);
		
		// Register EGL-specific built-in variable 'out'
		context.getFrameStack().put(new Variable("out", new EolNativeType(IOutputBuffer.class)));
		
		super.mainValidate();
		super.postValidate();

		return markers;
	}
	
}
