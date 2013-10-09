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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.FileManager;

import fr.inria.wimmics.prissma.selection.Decomposer;
import fr.inria.wimmics.prissma.selection.PrissmaProperties;
import fr.inria.wimmics.prissma.selection.entities.ContextUnit;
import fr.inria.wimmics.prissma.selection.entities.CtxUnitType;
import fr.inria.wimmics.prissma.selection.entities.DecompItem;
import fr.inria.wimmics.prissma.selection.entities.Decomposition;
import fr.inria.wimmics.prissma.selection.entities.Edge;
import fr.inria.wimmics.prissma.selection.entities.Prism;
import fr.inria.wimmics.prissma.selection.utilities.ContextUnitConverter;

public class DecomposerTest {
	
	private Logger LOG = LoggerFactory.getLogger(DecomposerTest.class);

	
	
	@Test
	public void testDecompose(){
		Decomposition expectedDecomp = createSimpleDecomposition();
		Model inputPrism = createSimpleModel();
		Decomposer decomposer = new Decomposer();
		Decomposition decomp = decomposer.decompose(inputPrism, new Decomposition());
		assertEquals(expectedDecomp, decomp);
		// add the same prism to see what happens
		decomp = decomposer.decompose(inputPrism, decomp);
		assertEquals(expectedDecomp, decomp);
		
	}
	
	class ModelMemoryTest{
		List<Model> prismsModels;
		
		public ModelMemoryTest(){
			this.prismsModels = new ArrayList<Model>();
		}
	}
	
	@Test
	public void testDecompositionMemorySize(){
		int ctxUnitCnt = 0;
		ModelMemoryTest mTest = new ModelMemoryTest();
		Decomposition decomp =  new Decomposition();
		Decomposer decomposer = new Decomposer();
		// read all prisms
		File dir = new File(PrissmaProperties.PRISM_PATH_TEST);
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".ttl")){
				Model inputPrism = ModelFactory.createDefaultModel();
				InputStream in = FileManager.get().open( PrissmaProperties.PRISM_PATH_TEST + file.getName() );
				if (in != null) {
					inputPrism.read(in, null,  "TURTLE");
					mTest.prismsModels.add(inputPrism);
					ctxUnitCnt += countCtxUnit(inputPrism, decomposer);
					System.out.print("Decomposing " + file.getName() + "... ");
					decomp = decomposer.decompose(inputPrism, decomp);
					System.out.println("Done.");
				}
			}
		}
		System.out.println("The decomposition contains " + decomp.getPrismCount() + " prisms\n");
		System.out.println("prisms ctxunit count: " + ctxUnitCnt);
		System.out.println("decomp ctxunits count: " + getCtxUnitCount(decomp));
		System.out.println("*Decomposition items: " + decomp.getSize());
		System.out.println("Total triples in test model: " + getTotalTriples(mTest.prismsModels));
		
	}
	
	
	private int getTotalTriples(List<Model> prismsModels) {
		int count = 0;
		for (Model prism : prismsModels) {
			count += prism.size();
		}
		return count;
	}


	private int getCtxUnitCount(Decomposition decomp) {
		int ctxUnitCnt = 0;
		for (DecompItem item: decomp.elements){
			if (item.isCtxUnit)
				ctxUnitCnt++;
		}
		return ctxUnitCnt;
	}

	private int countCtxUnit(Model inputPrism, Decomposer decomposer) {
		Model inputCopy = ModelFactory.createDefaultModel();
		inputCopy.add(inputPrism);
		Decomposition decomp = new Decomposition();
		ContextUnitConverter converter = new ContextUnitConverter();
		Prism prism = decomposer.getPrismObject(inputCopy);
		prism.rootNode = ContextUnitConverter.switchToClasses(prism.rootNode, decomp);
		converter.convertInputToUnits(prism.rootNode, decomp.substitutions.values());
		return converter.inputGraphContextUnits.size();
	}

	private Decomposition populateDecompositionFromDisk() {
		Decomposition decomp =  new Decomposition();
		Decomposer decomposer = new Decomposer();
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
		return decomp;
	}

	private Model createSimpleModel(){
		Model model = ModelFactory.createDefaultModel();
		Resource ctx1 = model.createResource(PrissmaProperties.DEFAULT + "ctx1");
		Resource usr1 = model.createResource(PrissmaProperties.DEFAULT + "usr1");
		model.add(ctx1, PrissmaProperties.pUsr, usr1);
		model.add(usr1, PrissmaProperties.pKnows, "computer programming" );
		return model;
	}


	private Decomposition createSimpleDecomposition(){
		
		
		
		Decomposition expectedDecomp = new Decomposition();
		// create some prissma property first
		Property prissmaUser = ResourceFactory.createProperty(PrissmaProperties.PRISSMA, "user");
		Property foafInterest = ResourceFactory.createProperty(PrissmaProperties.FOAF, "knows");
		
		// create ctx units
		ContextUnit ctxUnit1 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit1.instance = ResourceFactory.createResource( PrissmaProperties.DEFAULT + "ctx1");
		
		ContextUnit ctxUnit2 = new ContextUnit( CtxUnitType.ENTITY);
		ctxUnit2.instance = ResourceFactory.createResource( PrissmaProperties.DEFAULT + "usr1");
		
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
		expectedDecomp.elements.add(item0.id, item0);
		expectedDecomp.elements.add(item1.id, item1);
		expectedDecomp.elements.add(item2.id, item2);
		expectedDecomp.elements.add(item3.id, item3);
		expectedDecomp.elements.add(item4.id, item4);
		
		return expectedDecomp;
			
	}


}