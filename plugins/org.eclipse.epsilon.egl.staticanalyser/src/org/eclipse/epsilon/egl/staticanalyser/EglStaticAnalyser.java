package org.eclipse.epsilon.egl.staticanalyser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.module.IModule;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.egl.EglModule;
import org.eclipse.epsilon.egl.IEglModule;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.IEolModule;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.eol.staticanalyser.IModelFactory;


public class EglStaticAnalyser extends EolStaticAnalyser {
	
	public EglStaticAnalyser(IModelFactory modelFactory) {
		super(modelFactory);
	}
	
	@Override
	public List<ModuleMarker> validate(IModule imodule) {

		if (!(imodule instanceof IEglModule))
			return Collections.emptyList();

		this.module = (IEolModule) imodule;
		IEglModule eglModule = (IEglModule) imodule;
		

		super.preValidate(eglModule);
		super.mainValidate();
		super.postValidate();

		return errors;
	}
	
	public static void main(String[] args) throws Exception {
		Path eglPath = Paths.get("test.egl");
		if (!Files.exists(eglPath)) {
			throw new IllegalStateException(
				"Couldn't find test file at: " + eglPath.toAbsolutePath() + System.lineSeparator()
					+ "Create it at: plugins/org.eclipse.epsilon.egl.staticanalyser/test.egl");
		}

		String content = new String(Files.readAllBytes(eglPath), StandardCharsets.UTF_8);
		System.out.println("Loaded " + eglPath.toAbsolutePath());
		System.out.println("File content: " + content);
		
		IEglModule module = new EglModule();
		module.parse(content);
		EglStaticAnalyser analyser = new EglStaticAnalyser(null);
		List<ModuleMarker> markers = analyser.validate(module);
		if (markers.isEmpty()) {
			System.out.println("No problems found.");
		} else {
			System.out.println("Problems found:");
			for (ModuleMarker marker : markers) {
				System.out.println(marker);
			}
		}
	}
	
}