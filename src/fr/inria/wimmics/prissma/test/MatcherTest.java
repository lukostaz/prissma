/**
 * PRISSMA is a presentation-level framework for Linked Data adaptation.
 *
 * Copyright (C) 2013 Luca Costabello, v1.0
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package fr.inria.wimmics.prissma.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.FileManager;

import fr.inria.wimmics.prissma.selection.Decomposer;
import fr.inria.wimmics.prissma.selection.Matcher;
import fr.inria.wimmics.prissma.selection.PrissmaProperties;
import fr.inria.wimmics.prissma.selection.entities.ContextUnit;
import fr.inria.wimmics.prissma.selection.entities.CtxUnitType;
import fr.inria.wimmics.prissma.selection.entities.DecompItem;
import fr.inria.wimmics.prissma.selection.entities.Decomposition;
import fr.inria.wimmics.prissma.selection.entities.Edge;
import fr.inria.wimmics.prissma.selection.utilities.ContextUnitConverter;

public class MatcherTest {

	private Matcher testMatcher;
	
	
	
	@Test
	public void testConversion(){
		
		
		// creates input model
		Model model = ModelFactory.createDefaultModel();
		Resource ctx1 = model.createResource(PrissmaProperties.DEFAULT + "ctx1");
		Resource usr1 = model.createResource(PrissmaProperties.DEFAULT + "usr1");
		model.add(ctx1, PrissmaProperties.pUsr, usr1);
		model.add(usr1, PrissmaProperties.pKnows, "computer programming" );
		
		// create some prissma property first
		Property prissmaUser = ResourceFactory.createProperty(PrissmaProperties.PRISSMA, "user");
		Property foafInterest = ResourceFactory.createProperty(PrissmaProperties.FOAF, "knows");
		// create ctx units
		ContextUnit ctxUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit1.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "ctx1");
		ContextUnit ctxUnit2 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "usr1");
		ContextUnit ctxUnit3 = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3.instance = ResourceFactory.createPlainLiteral("computer programming");
		// edges
		Edge edge1 = new Edge();
		edge1.label = prissmaUser;
		edge1.v1 = ctxUnit1;
		edge1.v2 = ctxUnit2;
		Edge edge2 = new Edge();
		edge2.label = foafInterest;
		edge2.v1 = ctxUnit2;
		edge2.v2 = ctxUnit3;

		// Set input graph
		Set<ContextUnit> expectedCtxUnits = new HashSet<ContextUnit>();
		Set<Edge> expectedEdges = new HashSet<Edge>();
		expectedCtxUnits.add(ctxUnit1);
		expectedCtxUnits.add(ctxUnit2);
		expectedCtxUnits.add(ctxUnit3);
		expectedEdges.add(edge1);
		expectedEdges.add(edge2);
		
		this.testMatcher = new Matcher(new Decomposition());
		ContextUnitConverter c = new ContextUnitConverter();
		RDFNode ctxRoot = ContextUnitConverter.getRootCtxNode(model);
		c.convertInputToUnits(ctxRoot, new ArrayList<String>());
		testMatcher.inputGraphContextUnits = c.inputGraphContextUnits;
		testMatcher.inputGraphEdges = c.inputGraphEdges;
		
		assertTrue( testMatcher.inputGraphContextUnits.contains(ctxUnit1));
		assertTrue( testMatcher.inputGraphContextUnits.contains(ctxUnit2));
		assertTrue( testMatcher.inputGraphContextUnits.contains(ctxUnit3));
		assertEquals(expectedCtxUnits.size(), testMatcher.inputGraphContextUnits.size());
		assertTrue( testMatcher.inputGraphEdges.contains(edge1));
		assertTrue( testMatcher.inputGraphEdges.contains(edge2));
		assertEquals(expectedEdges.size(), testMatcher.inputGraphEdges.size());
	}
	
	
	@Test
	public void testConversionGEO(){
		
		
		double lat = 45.76849,
		 lon = 7.564732,
		 radius = 20;
		
		
		// creates input model
		Model model = ModelFactory.createDefaultModel();
		Resource ctx1 = model.createResource(PrissmaProperties.DEFAULT + "ctx1");
		Resource usr1 = model.createResource(PrissmaProperties.DEFAULT + "usr1");
		Resource env1 = model.createResource(PrissmaProperties.DEFAULT + "env1");
		Resource poi1 = model.createResource(PrissmaProperties.DEFAULT + "poi1");
		
		model.add(ctx1, PrissmaProperties.pUsr, usr1);
		model.add(usr1, PrissmaProperties.pKnows, "computer programming" );
		model.add(ctx1, PrissmaProperties.pEnv, env1);
		model.add(env1, PrissmaProperties.pPOI, poi1 );
		model.add(poi1, PrissmaProperties.pLat,  model.createTypedLiteral(lat));
		model.add(poi1, PrissmaProperties.pLon, model.createTypedLiteral(lon));
		model.add(poi1, PrissmaProperties.pRad, model.createTypedLiteral(radius));
		
		// create some prissma property first
		Model model2 = ModelFactory.createDefaultModel();
		Property prissmaUser = ResourceFactory.createProperty(PrissmaProperties.PRISSMA, "user");
		Property foafInterest = ResourceFactory.createProperty(PrissmaProperties.FOAF, "knows");
		// create ctx units
		ContextUnit ctxUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit1.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "ctx1");
		ContextUnit ctxUnit2 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "usr1");
		ContextUnit ctxUnit3 = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3.instance = ResourceFactory.createPlainLiteral("computer programming");
		ContextUnit envUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		envUnit1.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "env1");
		ContextUnit poiUnit1 = new ContextUnit( CtxUnitType.GEO);
		poiUnit1.instance = model2.createResource(PrissmaProperties.DEFAULT + "poi1")
									.addLiteral(PrissmaProperties.pLat, lat)
									.addLiteral(PrissmaProperties.pLat, lon)
									.addLiteral(PrissmaProperties.pRad, radius);
//		poiUnit1.instance = ResourceFactory.createResource("http://example/poi1")
//				.addLiteral(PrissmaProperties.pLat, lat)
//				.addLiteral(PrissmaProperties.pLat, lon)
//				.addLiteral(PrissmaProperties.pRad, radius);
		
		
		
		// edges
		Edge edge1 = new Edge();
		edge1.label = prissmaUser;
		edge1.v1 = ctxUnit1;
		edge1.v2 = ctxUnit2;
		Edge edge2 = new Edge();
		edge2.label = foafInterest;
		edge2.v1 = ctxUnit2;
		edge2.v2 = ctxUnit3;
		Edge edge3 = new Edge();
		edge3.label = PrissmaProperties.pEnv;
		edge3.v1 = ctxUnit1;
		edge3.v2 = envUnit1;
		Edge edge4 = new Edge();
		edge4.label = PrissmaProperties.pPOI;
		edge4.v1 = envUnit1;
		edge4.v2 = poiUnit1;
		
		// Set input graph
		Set<ContextUnit> expectedCtxUnits = new HashSet<ContextUnit>();
		Set<Edge> expectedEdges = new HashSet<Edge>();
		expectedCtxUnits.add(ctxUnit1);
		expectedCtxUnits.add(ctxUnit2);
		expectedCtxUnits.add(ctxUnit3);
		expectedCtxUnits.add(envUnit1);
		expectedCtxUnits.add(poiUnit1);
		
		expectedEdges.add(edge1);
		expectedEdges.add(edge2);
		expectedEdges.add(edge3);
		expectedEdges.add(edge4);
		
		this.testMatcher = new Matcher(new Decomposition());
		ContextUnitConverter c = new ContextUnitConverter();
		RDFNode ctxRoot = ContextUnitConverter.getRootCtxNode(model);
		c.convertInputToUnits(ctxRoot,  new ArrayList<String>());
		testMatcher.inputGraphContextUnits = c.inputGraphContextUnits;
		testMatcher.inputGraphEdges = c.inputGraphEdges;
		
		
		assertTrue( testMatcher.inputGraphContextUnits.contains(ctxUnit1));
		assertTrue( testMatcher.inputGraphContextUnits.contains(ctxUnit2));
		assertTrue( testMatcher.inputGraphContextUnits.contains(ctxUnit3));
		assertTrue( testMatcher.inputGraphContextUnits.contains(envUnit1));
		assertTrue( testMatcher.inputGraphContextUnits.contains(poiUnit1));
		assertEquals(expectedCtxUnits.size(), testMatcher.inputGraphContextUnits.size());
		assertTrue( testMatcher.inputGraphEdges.contains(edge1));
		assertTrue( testMatcher.inputGraphEdges.contains(edge2));
		assertTrue( testMatcher.inputGraphEdges.contains(edge3));
		assertTrue( testMatcher.inputGraphEdges.contains(edge4));
		assertEquals(expectedEdges.size(), testMatcher.inputGraphEdges.size());
	}
	
	
	@Test
	public void testSearchSinglePrismSuccess() {
		
		List<URI> expectedResult = new ArrayList<URI>();
		try {
			expectedResult.add(new URI(PrissmaProperties.DEFAULT + "prism1"));
		} catch (URISyntaxException e) {
		}
		
		this.testMatcher = new Matcher(new Decomposition());
		setupSearchSinglePrismSuccess();
		testMatcher.search(null);
		assertArrayEquals(expectedResult.toArray(), testMatcher.results.toArray()); 
		
	}
	
	@Test
	public void testSearchSinglePrismFail() {
		
		this.testMatcher = new Matcher(new Decomposition());
		setupSearchSinglePrismFail();
		testMatcher.search(null);
		assertTrue( testMatcher.results.isEmpty());
		
	}
	
	@Test
	public void testSearchSinglePrismSimilarSuccess() {
		
		List<URI> expectedResult = new ArrayList<URI>();
		try {
			expectedResult.add(new URI(PrissmaProperties.DEFAULT + "prism1"));
		} catch (URISyntaxException e) {
		}
		
		this.testMatcher = new Matcher(new Decomposition());
		setupSearchSinglePrismSimilar();
		testMatcher.search(null);
		assertArrayEquals(expectedResult.toArray(), testMatcher.results.toArray()); 
		
	}
	
	
	@Test
	public void testSearchSinglePrismModelInputSuccess() {
		
		// creates input model
		Model model = createSampleCtxInput();
		
		List<URI> expectedResult = new ArrayList<URI>();
		try {
			expectedResult.add(new URI(PrissmaProperties.DEFAULT + "prism1"));
		} catch (URISyntaxException e) {
		}
		
		this.testMatcher = new Matcher(new Decomposition());
		setupSearchSinglePrismSuccessModel();
		RDFNode ctxRoot = ContextUnitConverter.getRootCtxNode(model); 
		testMatcher.search(ctxRoot);
		assertArrayEquals(expectedResult.toArray(), testMatcher.results.toArray()); 
		
	}
	
	
	
	private Model createSampleCtxInput() {
		Model model = ModelFactory.createDefaultModel();
		Resource ctx1 = model.createResource(PrissmaProperties.DEFAULT + "ctx1");
		Resource usr1 = model.createResource(PrissmaProperties.DEFAULT + "usr1");
		model.add(ctx1, PrissmaProperties.pUsr, usr1);
		model.add(usr1, PrissmaProperties.pKnows, "computer programming" );
		return model;
	}


	@Test
	public void testSearchAndDecompositionSinglePrismSuccess(){
		
		String prism1URI = PrissmaProperties.DEFAULT + "prism1";
		Set<URI> expectedResult = new HashSet<URI>();
		try {
			expectedResult.add(new URI(prism1URI));
		} catch (URISyntaxException e) {
		}
		
		Model inputPrism = createSimpleModel();
		// add prism
		inputPrism.add(ResourceFactory.createResource(prism1URI), 
					   PrissmaProperties.pPurpose, 
					   ResourceFactory.createResource(PrissmaProperties.DEFAULT + "ctx1"));
		Decomposer decomposer = new Decomposer();
		Decomposition decomp = decomposer.decompose(inputPrism, new Decomposition());
		this.testMatcher = new Matcher(decomp);
		RDFNode ctxRoot = ContextUnitConverter.getRootCtxNode(inputPrism); 
		testMatcher.search(ctxRoot);
		Set<URI> prismURIs = testMatcher.results;
		
		assertEquals(expectedResult, prismURIs);
	}
	
	
	
	@Test
	public void testIOSearchAndDecompositionSinglePrismSuccess(){
		
		Model inputPrism = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open( PrissmaProperties.PRISM_PATH_TEST + PrissmaProperties.PRISM1_FILENAME );
		if (in != null) {
			inputPrism.read(in, null,  "TURTLE");
		}
		
		String prism1URI = PrissmaProperties.DEFAULT + "prism1";
		Set<URI> expectedResult = new HashSet<URI>();
		try {
			expectedResult.add(new URI(prism1URI));
		} catch (URISyntaxException e) {
		}
		
		Decomposer decomposer = new Decomposer();
		Decomposition decomp = decomposer.decompose(inputPrism, new Decomposition());
		this.testMatcher = new Matcher(decomp);
		RDFNode ctxRoot = ContextUnitConverter.getRootCtxNode(inputPrism); 
		testMatcher.search(ctxRoot);
		Set<URI> prismURIs = testMatcher.results;
		
		assertEquals(expectedResult, prismURIs);
	}
	
	
	@Test
	public void testIOSearchAndDecompositionMultiplePrismsSuccess(){
		
		// set expected result first
		String prism1URI = PrissmaProperties.DEFAULT + "prism2";
		Set<URI> expectedResult = new HashSet<URI>();
		try {
			expectedResult.add(new URI(prism1URI));
		} catch (URISyntaxException e) {
		}
		
		Decomposer decomposer = new Decomposer();
		Decomposition decomp = new Decomposition();
		
		// read all prisms
		File dir = new File(PrissmaProperties.PRISM_PATH_TEST);
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".ttl")){
				Model inputPrism = ModelFactory.createDefaultModel();
				InputStream in = FileManager.get().open( PrissmaProperties.PRISM_PATH_TEST + file.getName() );
				if (in != null) {
					inputPrism.read(in, null,  "TURTLE");
					decomp = decomposer.decompose(inputPrism, decomp);
				}
			}
		}

		// read input ctx
		Model actualCtx = ModelFactory.createDefaultModel();
//		InputStream in = FileManager.get().open( PrissmaProperties.ACTUAL_CTX_PATH_TEST + PrissmaProperties.ACTUAL_CTX_FILENAME );
		InputStream in = FileManager.get().open( PrissmaProperties.ACTUAL_CTX_PATH_TEST + "ctx2.ttl" );
		if (in != null) {
			actualCtx.read(in, null,  "TURTLE");
		}
		
		// search for actual ctx.
		this.testMatcher = new Matcher(decomp);
		RDFNode ctxRoot = ContextUnitConverter.getRootCtxNode(actualCtx); 
		testMatcher.search(ctxRoot);
		Set<URI> prismURIs = testMatcher.results;
		assertEquals(expectedResult, prismURIs);
	}
	
	
	@Test
	public void testIOSearchAndDecompositionMultiplePrismsGEOSuccess(){
		
//		int INDEX = 0;
		for (int INDEX = 8 ; INDEX < 10 ; INDEX++){
			
			// set expected result first
			String prism3URI = PrissmaProperties.DEFAULT + "prism0";
			Set<URI> expectedResult = new HashSet<URI>();
			try {
				expectedResult.add(new URI(prism3URI));
			} catch (URISyntaxException e) {
			}
			
			Decomposer decomposer = new Decomposer();
			Decomposition decomp = new Decomposition();
			
			// read all prisms
			File dir = new File(PrissmaProperties.PRISM_PATH_TEST);
			for (File file : dir.listFiles()) {
				if (file.getName().endsWith(".ttl")){
					Model inputPrism = ModelFactory.createDefaultModel();
					InputStream in = FileManager.get().open( PrissmaProperties.PRISM_PATH_TEST + file.getName() );
					if (in != null) {
						inputPrism.read(in, null,  "TURTLE");
						decomp = decomposer.decompose(inputPrism, decomp);
					}
				}
			}
	
			// read input ctx
			Model actualCtx = ModelFactory.createDefaultModel();
			InputStream in = FileManager.get().open( PrissmaProperties.ACTUAL_CTX_PATH_TEST + "ctx" + INDEX + ".ttl" );
			if (in != null) {
				actualCtx.read(in, null,  "TURTLE");
			}
			
			// search for actual ctx.
			this.testMatcher = new Matcher(decomp);
			RDFNode ctxRoot = ContextUnitConverter.getRootCtxNode(actualCtx); 
			ctxRoot = ContextUnitConverter.switchToClasses(ctxRoot, decomp);
			testMatcher.search(ctxRoot);
			Set<URI> prismURIs = testMatcher.results;
	//		assertEquals(expectedResult, prismURIs);
			URI targetPrismURI = null;
			try {
				targetPrismURI = new URI(prism3URI);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (prismURIs.contains(targetPrismURI))
				System.out.println("ctx" + INDEX +  " found");
			else
				System.err.println("ctx" + INDEX +  " NOT found");
		}
	}
	
	
	
	
	
	
	
	
	
	
	private Model createSimpleModel(){
		Model model = ModelFactory.createDefaultModel();
		Resource ctx1 = model.createResource(PrissmaProperties.DEFAULT + "ctx1");
		Resource usr1 = model.createResource(PrissmaProperties.DEFAULT + "usr1");
		model.add(ctx1, PrissmaProperties.pUsr, usr1);
		model.add(usr1, PrissmaProperties.pKnows, "computer programming" );
		return model;
	}
	
	
	
	
	
	/**
	 * Set a sample input graph. Same graph used in the decomposition.
	 * Sets test decomposition containing a single test graph.
	 * Creates a ctx1-->usr1-->"computer programming" structure.
	 */
	private void setupSearchSinglePrismSuccess(){
		
		// create some prissma property first
		Property prissmaUser = ResourceFactory.createProperty(PrissmaProperties.PRISSMA, "user");
		Property foafInterest = ResourceFactory.createProperty(PrissmaProperties.FOAF, "knows");
		
		// create ctx units
		ContextUnit ctxUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit1.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "ctx1");
		
		ContextUnit ctxUnit2 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "usr1");
		
		ContextUnit ctxUnit3 = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3.instance = ResourceFactory.createPlainLiteral("computer programming");
		
		// convert ctx units to decomposition elements elements list
		DecompItem item0 = new DecompItem(0);
		item0.ctxUnit = ctxUnit1;
		item0.isCtxUnit = true;
		
		DecompItem item1 = new DecompItem(1);
		item1.ctxUnit = ctxUnit2;
		item1.isCtxUnit = true;
		
		DecompItem item2 = new DecompItem(2);
		item2.ctxUnit = ctxUnit3;
		item2.isCtxUnit = true;
		
		
		//create other decomp items
		DecompItem item3 = new DecompItem(3);
		Edge edge1 = new Edge();
		edge1.label = foafInterest;
		edge1.v1 = ctxUnit2;
		edge1.v2 = ctxUnit3;
		item3.edges.add(edge1);
		item3.idAncestor1 = 1;
		item3.idAncestor2 = 2;
		
		DecompItem item4 = new DecompItem(4);
		Edge edge2 = new Edge();
		edge2.label = prissmaUser;
		edge2.v1 = ctxUnit1;
		edge2.v2 = ctxUnit2;
		item4.edges.add(edge2);
		item4.idAncestor1 = 0;
		item4.idAncestor2 = 3;
		item4.prismURISet = new HashSet<URI>();
		try {
			item4.prismURISet.add(new URI(PrissmaProperties.DEFAULT + "prism1"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		// add converted ctx units to decomposition
		testMatcher.decomp.elements.add(item0.id, item0);
		testMatcher.decomp.elements.add(item1.id, item1);
		testMatcher.decomp.elements.add(item2.id, item2);
		testMatcher.decomp.elements.add(item3.id, item3);
		testMatcher.decomp.elements.add(item4.id, item4);
			
				
		// Set input graph
		testMatcher.inputGraphContextUnits.add(ctxUnit1);
		testMatcher.inputGraphContextUnits.add(ctxUnit2);
		testMatcher.inputGraphContextUnits.add(ctxUnit3);
		
		testMatcher.inputGraphEdges.add(edge1);
		testMatcher.inputGraphEdges.add(edge2);
	}
	
	/**
	 * Set a sample input graph. Same graph used in the decomposition.
	 * Sets test decomposition containing a single test graph.
	 * Creates a ctx1-->usr1-->"computer programming" structure.
	 */
	private void setupSearchSinglePrismFail(){
		
		// create some prissma property first
		Property prissmaUser = ResourceFactory.createProperty(PrissmaProperties.PRISSMA, "user");
		Property foafInterest = ResourceFactory.createProperty(PrissmaProperties.FOAF, "knows");
		
		
		// create ctx units
		ContextUnit ctxUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit1.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "ctx1");
		
		ContextUnit ctxUnit2 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "usr1");
		
		ContextUnit ctxUnit3 = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3.instance = ResourceFactory.createPlainLiteral("computer programming");
		
		ContextUnit ctxUnit2Wrong = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2Wrong.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "WRONG-USER");
		
		ContextUnit ctxUnit3Similar = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3Similar.instance = ResourceFactory.createPlainLiteral("computer programming languages");
		
		
		// convert ctx units to decomposition elements elements list
		DecompItem item1 = new DecompItem(0);
		item1.ctxUnit = ctxUnit1;
		item1.isCtxUnit = true;
		
		DecompItem item2 = new DecompItem(1);
		item2.ctxUnit = ctxUnit2;
		item2.isCtxUnit = true;
		
		DecompItem item3 = new DecompItem(2);
		item3.ctxUnit = ctxUnit3;
		item3.isCtxUnit = true;
		
		
		//create other decomp items
		DecompItem item4 = new DecompItem(3);
		Edge edge1 = new Edge();
		edge1.label = prissmaUser;
		edge1.v1 = ctxUnit1;
		edge1.v2 = ctxUnit2;
		item4.edges.add(edge1);
		item4.idAncestor1 = 0;
		item4.idAncestor2 = 3;
		
		DecompItem item5 = new DecompItem(4);
		Edge edge2 = new Edge();
		edge2.label = foafInterest;
		edge2.v1 = ctxUnit2;
		edge2.v2 = ctxUnit3;
		item5.edges.add(edge2);
		item5.idAncestor1 = 1;
		item5.idAncestor2 = 2;
		item5.prismURISet = new HashSet<URI>();
		try {
			item5.prismURISet.add(new URI(PrissmaProperties.DEFAULT + "prism1"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		// add converted ctx units to decomposition
		testMatcher.decomp.elements.add(item1.id, item1);
		testMatcher.decomp.elements.add(item2.id, item2);
		testMatcher.decomp.elements.add(item3.id, item3);
		testMatcher.decomp.elements.add(item4.id, item4);
		testMatcher.decomp.elements.add(item5.id, item5);
			
				
		// Set input graph
		testMatcher.inputGraphContextUnits.add(ctxUnit1);
		testMatcher.inputGraphContextUnits.add(ctxUnit2Wrong);
		testMatcher.inputGraphContextUnits.add(ctxUnit3);
		
		testMatcher.inputGraphEdges.add(edge1);
		testMatcher.inputGraphEdges.add(edge2);
	}
	
	
	/**
	 * Set a sample input graph. Same graph used in the decomposition.
	 * Sets test decomposition containing a single test graph.
	 * Creates a ctx1-->usr1-->"computer programming" structure.
	 */
	private void setupSearchSinglePrismSimilar(){
		
		// create some prissma property first
		Property prissmaUser = ResourceFactory.createProperty(PrissmaProperties.PRISSMA, "user");
		Property foafInterest = ResourceFactory.createProperty(PrissmaProperties.FOAF, "knows");
		
		
		// create ctx units
		ContextUnit ctxUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit1.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "ctx1");
		
		ContextUnit ctxUnit2 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "usr1");
		
		ContextUnit ctxUnit3 = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3.instance = ResourceFactory.createPlainLiteral("computer programming");
		
		ContextUnit ctxUnit2Wrong = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2Wrong.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "WRONG-USER");
		
		ContextUnit ctxUnit3Similar = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3Similar.instance =  ResourceFactory.createPlainLiteral("computer programming languages");
		
		
		// convert ctx units to decomposition elements elements list
		DecompItem item1 = new DecompItem(0);
		item1.ctxUnit = ctxUnit1;
		item1.isCtxUnit = true;
		
		DecompItem item2 = new DecompItem(1);
		item2.ctxUnit = ctxUnit2;
		item2.isCtxUnit = true;
		
		DecompItem item3 = new DecompItem(2);
		item3.ctxUnit = ctxUnit3;
		item3.isCtxUnit = true;
		
		
		//create other decomp items
		DecompItem item4 = new DecompItem(3);
		Edge edge1 = new Edge();
		edge1.label = prissmaUser;
		edge1.v1 = ctxUnit1;
		edge1.v2 = ctxUnit2;
		item4.edges.add(edge1);
		item4.idAncestor1 = 0;
		item4.idAncestor2 = 3;
		
		DecompItem item5 = new DecompItem(4);
		Edge edge2 = new Edge();
		edge2.label = foafInterest;
		edge2.v1 = ctxUnit2;
		edge2.v2 = ctxUnit3;
		item5.edges.add(edge2);
		item5.idAncestor1 = 1;
		item5.idAncestor2 = 2;
		item5.prismURISet = new HashSet<URI>();
		try {
			item5.prismURISet.add(new URI(PrissmaProperties.DEFAULT + "prism1"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		// add converted ctx units to decomposition
		testMatcher.decomp.elements.add(item1.id, item1);
		testMatcher.decomp.elements.add(item2.id, item2);
		testMatcher.decomp.elements.add(item3.id, item3);
		testMatcher.decomp.elements.add(item4.id, item4);
		testMatcher.decomp.elements.add(item5.id, item5);
			
				
		// Set input graph
		testMatcher.inputGraphContextUnits.add(ctxUnit1);
		testMatcher.inputGraphContextUnits.add(ctxUnit2);
		testMatcher.inputGraphContextUnits.add(ctxUnit3Similar);
		
		testMatcher.inputGraphEdges.add(edge1);
		testMatcher.inputGraphEdges.add(edge2);
	}
	

	
	
	/**
	 * Set a sample input graph. Same graph used in the decomposition.
	 * Sets test decomposition containing a single test graph.
	 * Creates a ctx1-->usr1-->"computer programming" structure.
	 */
	private void setupSearchSinglePrismSuccessModel(){
		
		
		// create some prissma property first
		Property prissmaUser = ResourceFactory.createProperty(PrissmaProperties.PRISSMA, "user");
		Property foafInterest = ResourceFactory.createProperty(PrissmaProperties.FOAF, "knows");
		
		// create ctx units
		ContextUnit ctxUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit1.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "ctx1");
		
		ContextUnit ctxUnit2 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2.instance = ResourceFactory.createResource(PrissmaProperties.DEFAULT + "usr1");
		
		ContextUnit ctxUnit3 = new ContextUnit( CtxUnitType.STRING);
		ctxUnit3.instance = ResourceFactory.createPlainLiteral("computer programming");
		
		// convert ctx units to decomposition elements elements list
		DecompItem item1 = new DecompItem(1);
		item1.ctxUnit = ctxUnit1;
		item1.isCtxUnit = true;
		
		DecompItem item2 = new DecompItem(2);
		item2.ctxUnit = ctxUnit2;
		item2.isCtxUnit = true;
		
		DecompItem item3 = new DecompItem(3);
		item3.ctxUnit = ctxUnit3;
		item3.isCtxUnit = true;
		
		
		//create other decomp items
		DecompItem item4 = new DecompItem(4);
		Edge edge1 = new Edge();
		edge1.label = prissmaUser;
		edge1.v1 = ctxUnit1;
		edge1.v2 = ctxUnit2;
		item4.edges.add(edge1);
		item4.idAncestor1 = 1;
		item4.idAncestor2 = 2;
		
		DecompItem item5 = new DecompItem(5);
		Edge edge2 = new Edge();
		edge2.label = foafInterest;
		edge2.v1 = ctxUnit2;
		edge2.v2 = ctxUnit3;
		item5.edges.add(edge2);
		item5.idAncestor1 = 4;
		item5.idAncestor2 = 3;
		item5.prismURISet = new HashSet<URI>();
		try {
			item5.prismURISet.add(new URI(PrissmaProperties.DEFAULT + "prism1"));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		// add converted ctx units to decomposition
		testMatcher.decomp.elements.add(item1.id, item1);
		testMatcher.decomp.elements.add(item2.id, item2);
		testMatcher.decomp.elements.add(item3.id, item3);
		testMatcher.decomp.elements.add(item4.id, item4);
		testMatcher.decomp.elements.add(item5.id, item5);
			
	}
	
	
	
	
}
