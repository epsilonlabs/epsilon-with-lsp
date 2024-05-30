package org.eclipse.epsilon.eol.staticanalyser.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.common.module.ModuleMarker;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.staticanalyser.EolStaticAnalyser;
import org.eclipse.epsilon.eol.staticanalyser.StaticModelFactory;
import org.eclipse.epsilon.eol.types.EolType;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class EolStaticAnalyserTests {
	static int ident = 0;

	@BeforeClass
	public static void registerEcore() {
		EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
		registerPackage("src/org/eclipse/epsilon/eol/staticanalyser/tests/meta1.ecore");
	}
	
	public static void registerPackage(String path) {
        // Create a ResourceSet and register the default resource factory
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new XMIResourceFactoryImpl());
        
        // Load the .ecore file
        URI fileURI = URI.createFileURI(path);
        Resource resource = resourceSet.getResource(fileURI, true);
        
        // Extract the EPackage from the loaded resource
        EObject eObject = resource.getContents().get(0);
        if (eObject instanceof EPackage) {
            EPackage ePackage = (EPackage) eObject;
            
            // Register the EPackage
            EPackage.Registry.INSTANCE.put(ePackage.getNsURI(), ePackage);
        }
	}
	
	@Test
	public void testModelElementTypeVariableDeclaration() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass;");
		assertValid(st.toString());
	}
	
	@Test
	public void testModelElementTypeVariableDeclarationError() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClss;");
		assertErrorMessage(st.toString(), "Unknown type M!EClss");
	}
	
	@Test
	public void testModelElementTypeAssignment() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass = new M!EClass;");
		assertValid(st.toString());
	}
	
	@Test
	public void testModelElementTypeAssignmentError() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass = new M!EPackage;");
		assertErrorMessage(st.toString(), "M!EPackage cannot be assigned to M!EClass");
	}
	
	@Test
	public void testModelElementTypePropertyAssignmentError() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass = new M!EClass;");
		st.append("i.names = 'className';");
		assertErrorMessage(st.toString(), "Structural feature names not found in type EClass");
	}
	
	@Test
	public void testModelElementTypePropertyAssignmentError2() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass = new M!EClass;");
		st.append("i.name = 2;");
		assertErrorMessage(st.toString(), "Integer cannot be assigned to String");
	}
	
	@Test
	public void testModelAliases() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M alias X driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:X!EClass = new X!EClass;");
		assertValid(st.toString());
	}
	
	@Test
	@Ignore
	public void testDuplicateModelDeclaration() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass = new M!EClass;");
		assertErrorMessage(st.toString(), "");
	}
	
	@Test
	public void testMultipleModelAliases() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M alias X,Y driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:Y!EClass = new Y!EClass;");
		assertValid(st.toString());
	}
	
	@Test
	public void testModelAliasesError() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M alias X driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:X!EClss;");
		assertErrorMessage(st.toString(), "Undefined variable or type X!EClss");
	}
	
	@Test
	@Ignore
	public void dummy() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M alias X driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass;");
		st.append("i = new EClass();");
//		printTree(st.toString());
		assertValid(st.toString());
	}
	
	@Test
	@Ignore // ignore until we add builtin operations back
	public void testTuplePropertyError() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var t:Tuple = new Tuple(name = 'bob');");
		st.append("t.names.println();");
		assertErrorMessage(st.toString(), "Tuple t does not have property names");
	}
	
	
	@Test
	@Ignore
	public void testNativeType() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var r = new Native('java.util.Random');");
//		st.append("r.nextint()");
//		printTree(st.toString());
		assertValid(st.toString());
	}
	
	@Test
	@Ignore
	public void testUseAfterDelete() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};");
		st.append("var i:M!EClass = new M!EClass;");
		st.append("delete i;");
		st.append("i.name = 'className';");
		assertErrorMessage(st.toString(), "Cannot access object i after deletion");
	}
	
	@Test
	@Ignore
	public void testUseAfterStochasticDelete() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='http://www.eclipse.org/emf/2002/Ecore'};\n");
		st.append("var i:M!EClass = new M!EClass;\n");
		st.append("var b = new Bag();\n");
		st.append("var r = new Native('java.util.Random');\n");
		st.append("if (r.nextint(99) > 50){delete i;}\n");
		st.append("i.name = 'className';");
		assertErrorMessage(st.toString(), "Cannot access object i after deletion");
	}

	@Test
	@Ignore //ignore until we add builtin operations back
	public void testPrimitiveTypesVariableDeclaration() throws Exception {
		assertValid("var i : Integer; (/*Integer*/i).println();");
		assertValid("var b : Boolean; (/*Boolean*/b).println();");
		assertValid("var s : String; (/*String*/s).println();");
		assertValid("var r : Real; (/*Real*/r).println();");
	}

	@Test
	public void testPrimitiveTypesAssignmentExpression() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var i : Integer = 4;");
		st.append("var s : String = 'test';");
		st.append("(/*Integer*/i) = 5;");
		st.append("var a : Any = true;");
		st.append("(/*Any*/ a) = (/*String*/s);");
		assertValid(st.toString());
	}

	@Test
	public void testCollectionTypesAssignmentExpression() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var col : Collection<Integer> = Collection{0..9};\n");
		st.append("var seq : Sequence<Integer> = Sequence{0..9};\n");
		st.append("var bg : Bag<Integer> = Bag{0..9};\n");
		st.append("(/*Collection<Integer>*/col) = (/*Bag<Integer>*/bg);\n");
		st.append("(/*Collection<Integer>*/col) = (/*Sequence<Integer>*/seq);\n");
		assertValid(st.toString());
	}
	
	@Test
	public void operationReturnsSequenceType() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var v : Integer;\n");
		st.append("v = op().f();\n");
		st.append("operation op():Sequence<Integer>{return Sequence{1,2,3};}\n");
		st.append("operation Sequence f():Integer{return 1}\n");
		assertValid(st.toString());
	}
	
	@Test
	public void operationReturnsSequenceOfSequenceType() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var v:Sequence<Sequence<Integer>>;\n");
		st.append("v = op();\n");
		st.append("operation op():Sequence<Sequence<Integer>>{return Sequence{Sequence{1,2,3}};}\n");
		assertValid(st.toString());
	}

	@Test
	public void testPrimitiveTypesAssignmentExpressionErrorMessage() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var i : Integer = 4;");
		st.append("var s : String = 'test';");
		st.append("i = s;");
		assertErrorMessage(st.toString(), "String cannot be assigned to Integer");
	}
	
	@Test
	@Ignore //ignore until we add builtin operations back
	public void testBuiltinMethods() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("var s : String = 'Test';");
		st.append("s.firstToLowerCase();");
		assertValid(st.toString());
	}
	
	@Test
	public void testMultiplePossibleTypes() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='sa'};");
		st.append("for (a in A.all) {\n"
				+ "    var v1 = a.foo();\n"
				+ "    var v2 = v1.foo();\n"
				+ "}\n"
				+ "operation B foo() : B {return self;}\n"
				+ "operation C foo() : C {return self;}");
		assertValid(st.toString());
	}
	
	@Test
	public void testTypeResolutionWithAll() throws Exception {
		StringBuffer st = new StringBuffer();
		st.append("model M driver EMF {nsuri='sa'};");
		st.append("for (a in B.all) {\n"
				+ "    var v = (/*B*/a);\n"
				+ "}\n");
		assertValid(st.toString());
	}

	public void assertValid(String eol) throws Exception {
		EolModule module = new EolModule();
		module.parse(eol);
		EolStaticAnalyser staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
		List<ModuleMarker> errors = staticAnalyser.validate(module);
		String messages = errors.stream()
                .map(ModuleMarker::getMessage)
                .collect(Collectors.joining("\n"));
		assertEquals("Unexpected number of errors\n" + messages + "\n", 0, errors.size());
		visit(module.getChildren());
	}

	public void assertErrorMessage(String eol, String message) throws Exception {
		EolModule module = new EolModule();
		module.parse(eol);
		EolStaticAnalyser staticAnalyser = new EolStaticAnalyser(new StaticModelFactory());
		List<ModuleMarker> errors = staticAnalyser.validate(module);
		
		assertEquals("unexpected number of errors (" + errors.size() + ") in eol module", 1, errors.size());
		assertEquals(message, errors.get(0).getMessage());
	}

	protected void visit(List<ModuleElement> elements) {
		for (ModuleElement element : elements) {
			if (!element.getComments().isEmpty()) {
				assertEquals(element.getComments().get(0).toString(), getResolvedType(element).toString());
			}
			visit(element.getChildren());
		}
	}

	protected EolType getResolvedType(ModuleElement element) {
		return (EolType) element.getData().get("resolvedType");
	}
	
//	public void printTree(ModuleElement parent) {
//		System.out.println("  ".repeat(ident) + parent.getClass().getSimpleName());
//		ident += 1;
//		for (ModuleElement m : parent.getChildren()) {
//			printTree(m);
//		}
//		ident -= 1;
//	}
	
//	public void printTree (String eol) throws Exception {
//		EolModule module = new EolModule();
//		module.parse(eol);
//		printTree(module);
//		
//	}
}
