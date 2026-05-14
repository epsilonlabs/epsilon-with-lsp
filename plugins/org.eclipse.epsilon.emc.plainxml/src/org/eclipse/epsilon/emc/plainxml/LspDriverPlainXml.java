package org.eclipse.epsilon.emc.plainxml;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.m3.IMetamodel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;



public class LspDriverPlainXml extends PlainXmlModel {
	
	private boolean CONSOLE = true;
	private PlainXmlModelMetamodel metaModel;
	
	@Override
	public IMetamodel getMetamodel(StringProperties properties, IRelativePathResolver pathResolver) {
		
		System.out.println("\ngetMetaModel()");
		if(this.isLoaded() == false) {
			try {
				this.load(properties,pathResolver);
				if (CONSOLE) {
					System.out.println("\n [ ! MODEL LOAD ! ] properties : " + properties);
					System.out.println("  THIS : " + this.isLoaded());
					System.out.println("  THIS root : " + this.getRoot());
					System.out.println("  THIS xmldoc : " + this.getXml());
				}
			} catch (EolModelLoadingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		if (null == metaModel) {
			if (CONSOLE) {
				System.out.println("Create MetaModel");
			}	
			metaModel = new PlainXmlModelMetamodel(this, properties, pathResolver, getName());
			//this.dispose();
		}
		
		return metaModel;
	}
	
	
	
}
