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

package fr.inria.wimmics.prissma.selection;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import fr.inria.wimmics.prissma.selection.entities.ContextUnit;
import fr.inria.wimmics.prissma.selection.entities.DecompItem;
import fr.inria.wimmics.prissma.selection.entities.Decomposition;
import fr.inria.wimmics.prissma.selection.entities.Edge;
import fr.inria.wimmics.prissma.selection.entities.Prism;
import fr.inria.wimmics.prissma.selection.exceptions.CuttingEdgeException;
import fr.inria.wimmics.prissma.selection.utilities.ContextUnitConverter;

public class Decomposer {

	private  Logger LOG = LoggerFactory.getLogger(Decomposer.class);

	
	public Decomposer() {
		super();
	}

	
	
	/**
	 * Wrapper method and entry point for decomposition procedure.
	 * 1) Trim fresnel data from prism
	 * 2) Substitute intermediate entities w/ their class
	 * 3) Apply recursive decomp method
	 * 
	 * @param model
	 * @param decomp
	 * @return
	 */
	public Decomposition decompose(Model prismModel, Decomposition decomp){
		Prism prism = getPrismObject(prismModel);
		prism.rootNode = ContextUnitConverter.switchToClasses(prism.rootNode, decomp);
		decomp = decomposeMain(prism.rootNode, prism.prismURI, decomp);
		return decomp;
	}
	

	


	/**
	 * Detects prism URI and filter out its direct properties.
	 * @param prismModel
	 * @return
	 */
	public Prism getPrismObject(Model prismModel) {
		Prism prism = new Prism();
		List<Statement> ignoreSt = new  ArrayList<Statement>();
		try {
			prism.prismURI = null;
			Resource prismRes = null;
			
			StmtIterator it = prismModel.listStatements( 
					new SimpleSelector((Resource)null, PrissmaProperties.pPurpose, (RDFNode) null) );
			while (it.hasNext()){
				Statement st = it.next();
				prismRes = st.getSubject();
				prism.prismURI = new URI(prismRes.getURI());
			}
			
			if (prismRes != null){
				// remove direct prissma:Prism properties (e.g. prissma:purpose)
				it = prismModel.listStatements(new SimpleSelector(prismRes, (Property) null, (RDFNode) null) );
				while (it.hasNext()){
					ignoreSt.add(it.next());
				}
				// detect triples w/ prissma:Prism as object and get the subjects (Lenses and Formats)
				// removes Lenses and Formats triples for each lens and format
				Resource list = null;
				it = prismModel.listStatements(new SimpleSelector((Resource) null, (Property) null, prismRes) );
				while (it.hasNext()){
					Statement st = it.next();
					Resource lensOrFormat = st.getSubject();
					StmtIterator it2 = prismModel.listStatements(new SimpleSelector(lensOrFormat, (Property) null,  (RDFNode) null));
					while (it2.hasNext()){
						Statement st2 = it2.next();
						// is it is a bag (fresnel showproperties/ hideproperties)
						if (st2.getPredicate().equals(PrissmaProperties.pShowProperties) ||
							st2.getPredicate().equals(PrissmaProperties.pHideProperties)){
							list = st2.getObject().asResource();
							
							StmtIterator it3 = prismModel.listStatements(new SimpleSelector(list, (Property) null,  (RDFNode) null));
							while (it3.hasNext()){
								Statement st3 = it3.next();
								ignoreSt.add(st3);
							}
						}
						ignoreSt.add(st2);
					}
				}
			}
			
			StmtIterator it3 = prismModel.listStatements(new SimpleSelector((Resource) null, PrissmaProperties.pSpurious1,  (RDFNode) null));
			while (it3.hasNext()){
				Statement st3 = it3.next();
				ignoreSt.add(st3);
			}
			it3 = prismModel.listStatements(new SimpleSelector((Resource) null, PrissmaProperties.pSpurious2,  (RDFNode) null));
			while (it3.hasNext()){
				Statement st3 = it3.next();
				ignoreSt.add(st3);
			}
			
			
			prismModel.remove(ignoreSt);
			
			
			prism.rootNode = ContextUnitConverter.getRootCtxNode(prismModel);
			
			if (prism.prismURI == null){
				LOG.error("Prism URI not found. ");
			}
			
			return prism;
			
		} catch (URISyntaxException e) {
			LOG.error("Error retrieving Prism URI: " + e.getMessage());
			return prism;
		}
		
	}





	public URI getPrismURI(Model prismModel) {
		
		try {
			Resource prismRes = null;
			StmtIterator it = prismModel.listStatements( 
					new SimpleSelector((Resource)null, PrissmaProperties.pPurpose, (RDFNode) null) );
			while (it.hasNext()){
				Statement st = it.next();
				prismRes = st.getSubject();
			}
			return new URI(prismRes.getURI());
		} catch (URISyntaxException e) {
			LOG.error("Error retrieving URI of Prism: " + e.getMessage());
			return null;
		}
		
	}

	/**
	 * The decomposition main method
	 * @param prismURL 
	 * @return
	 */
	private Decomposition decomposeMain(RDFNode g, URI prismURI, Decomposition decomp){
		
		RDFNode sMax = null, 
			    gMinusSmax = null;
		Edge e = null;
		
		// if g is ctxunit, it cannot be decomposed any further.
		if (!ContextUnitConverter.isCtxUnit(g)){
			sMax = searchSmax(decomp, g);
			
			// if g is already in the decomposition, exit.
			if (ContextUnitConverter.areIsomorphic(g, sMax)){
				if (prismURI != null && !prismURI.equals("")){
					int id = decomp.getItem(g);
					decomp.elements.get(id).prismURISet.add(prismURI);
				}
				return decomp;
			}
			
			// choose and recursively decompose sMax
			if (sMax == null){
				try {
					e = chooseCuttingEdge(g, decomp.substitutions.values());
					sMax = createSMax(e, g);
					gMinusSmax = difference(g, sMax, e);
				} catch (CuttingEdgeException e1) {
					LOG.error("Error choosing cutting edge in decomposition. " + e1.getMessage() );
					return null;
				}
				decomp = decomposeMain(sMax, null,  decomp);
				decomp = decomposeMain(gMinusSmax, null, decomp);
				// add sMax to decomposition
				updateDecomp(decomp, g, sMax, gMinusSmax, e, prismURI);
			}
			else{
				RDFNode ancestor1 = sMax;
				List<Edge> edges = getConnectingEdges(g, sMax, decomp.substitutions.values());
				Iterator<Edge> it = edges.iterator();
				while (it.hasNext()){
					URI currURI = null;
					Edge eConn = it.next();
					RDFNode pathNode = getPath(g, eConn, edges);
					gMinusSmax = difference(pathNode, sMax, eConn);
					decomp = decomposeMain(gMinusSmax, null, decomp);
					// add gMinusSmax decomp element to decomposition, but put prismURI only if it the last one (the real prism)
					if (!it.hasNext() || edges.size() == 1)
						currURI = prismURI;
					DecompItem lastAddedEl = updateDecomp(decomp, g, ancestor1, gMinusSmax, eConn, currURI);
					ancestor1 = decomp.getReconstructedModel(lastAddedEl);
				}
			}
		} else
			// add ctxunit to decomp
			updateDecomp(decomp, g, sMax, gMinusSmax, e, prismURI);
		
		return decomp;
	}
	

	private RDFNode getPath(RDFNode g, Edge edge, List<Edge> connEdges) {
		
		// init
		Model mG = g.getModel();
		if (mG == null)
			return null;
		if (mG.isEmpty())
			return g;
		Model mCopy = ModelFactory.createDefaultModel();
		mCopy.add(mG.listStatements());
		List<Resource> objects = new ArrayList<Resource>();
		
		// clean below
		StmtIterator it = mCopy.listStatements(edge.v1.instance.asResource(),
				(Property) null, (RDFNode) null);
		while (it.hasNext()) {
			Statement st = it.next();
			Property pred = st.getPredicate();
			RDFNode obj = st.getObject();
			if (!(pred.equals(edge.label) && obj.equals(edge.v2.instance))){
				if (obj.isResource())
					objects.add(obj.asResource());
				it.remove();
			}
		}
		for (Resource obj : objects) 
			removeStatements(mCopy, obj, null);
		
		
		// clean outside of smax
		for (Edge otherConnEdge : connEdges) {
			if (!otherConnEdge.equals(edge) && !otherConnEdge.v1.instance.equals(edge.v1.instance)){
				removeStatements(mCopy, otherConnEdge.v1.instance,otherConnEdge);
				mCopy.remove(otherConnEdge.v1.instance.asResource(), 
						otherConnEdge.label, 
						otherConnEdge.v2.instance);
			}
		}
		
		// return cleaned path
		return ContextUnitConverter.getRoot(mCopy, true);
	}
	
	
	private DecompItem updateDecomp(Decomposition decomp, RDFNode g, RDFNode sMax,
			RDFNode gMinusSmax, Edge e, URI prismURI) {

		DecompItem gDecompItem = new DecompItem(decomp, g, sMax, gMinusSmax, e, prismURI);
		if (!decomp.elements.contains(gDecompItem)){
			decomp.elements.add(decomp.idCounter, gDecompItem);
			decomp.idCounter ++;
		}
		
		return gDecompItem;
		
	}



	/**
	 * Searches for the edge that connects sMax w/ the rest of the input graph.
	 * @param g
	 * @param sMax
	 * @return
	 */
	private List<Edge> getConnectingEdges(RDFNode g, RDFNode sMax, Collection<String> RDFclasses) {

		List<Edge> connectingEdges = new ArrayList<Edge>();
		
		Model mSMax = sMax.getModel();
		Model mG = g.getModel();
		Statement sConn = null;
		List<Statement> sConnList = new ArrayList<Statement>();
		// if smax is ctxunit
		if (mSMax == null || mSMax.isEmpty()) {
			// check for subject connection ( do it only if sMax can be subject, i.e. it's a resource)
			if (sMax.isResource()) {
				StmtIterator it = mG.listStatements(sMax.asResource(),
						(Property) null, (RDFNode) null);
				while (it.hasNext()) {
					Statement st = it.next();
					sConn = ResourceFactory.createStatement(
							st.getSubject(), st.getPredicate(),
							st.getObject());
					sConnList.add(sConn);
				}
			}
			// now scan for object 
			StmtIterator it = mG.listStatements((Resource) null,
					(Property) null, sMax);
			while (it.hasNext()) {
				Statement st = it.next();
				sConn = ResourceFactory.createStatement(st.getSubject(),
						st.getPredicate(), st.getObject());
				sConnList.add(sConn);
			}
		} 
		// if smax is NOT ctxunit
		else {
			// check each node in Smax for outgoing/incoming connection
			StmtIterator itSMax = mSMax.listStatements();
			while (itSMax.hasNext()) {
				Statement s = itSMax.next();
				Resource sub = s.getSubject();
				RDFNode obj = s.getObject();
				// check subject, first for outgoing...
				StmtIterator itG = mG.listStatements(sub, (Property) null,(RDFNode) null);
				while (itG.hasNext()) {
					sConn = itG.next();
					// check if it is a different property because g contains sMAx, therefore also all its triples
					if (!mSMax.contains(sConn) && !sConnList.contains(sConn))
						sConnList.add(sConn);
				}
				// ... then for incoming connections
				StmtIterator itOut = mG.listStatements((Resource)null, (Property) null,sub);
				while (itOut.hasNext()) {
					sConn = itOut.next();
					// check if it a different property because g contains sMAx, therefore also all its triples
					if (!mSMax.contains(sConn) && !sConnList.contains(sConn))
						sConnList.add(sConn);
				}
				
				// check object for outgoing 
				if (obj.isResource()) {
					Resource objRes = obj.asResource();
					itG =  mG.listStatements(objRes, (Property) null,(RDFNode) null);
					while (itG.hasNext()) {
						sConn = itG.next();
						// check if it a different property because g contains sMAx, therefore also all its triples
						if (!mSMax.contains(sConn) && !sConnList.contains(sConn))
							sConnList.add(sConn);
					}
				}
			}
		}
		
		// wrap into Edge instances
		for (Statement sConnFound : sConnList) {
			Edge e = new Edge();
			e.label = sConnFound.getPredicate();
			ContextUnitConverter c = new ContextUnitConverter();
			c.convertInputToUnits(sConnFound.getSubject(), RDFclasses);
			for (ContextUnit cu : c.inputGraphContextUnits){
				if (cu.instance.equals(sConnFound.getSubject())) 
					e.v1 = cu;
			}
				
			c = new ContextUnitConverter();
			c.convertInputToUnits(sConnFound.getObject(), RDFclasses);
			for (ContextUnit cu : c.inputGraphContextUnits){
				if (cu.instance.equals(sConnFound.getObject())) 
					e.v2 = cu;
			}
				
			
			connectingEdges.add(e);
		}
			
		return connectingEdges;
	}



	/**
	 * Search decomposition for largest element sMax that is shared w/ input.
	 * Uses jena triple-based subgraph procedure.
	 * @param decomp
	 * @param g
	 * @return
	 */
	private RDFNode searchSmax(Decomposition decomp, RDFNode g) {
		RDFNode sMax = null;
		
		for ( DecompItem element : decomp.elements) {
			// size of decomp element is the # of context units that are contained in it.
			RDFNode elementNode = decomp.getReconstructedModel(element);
			if ( isSubgraphOfTripleBased(elementNode, g) &&
				 (sMax == null || 
				  getSize(sMax, decomp.substitutions.values()) < getSize(elementNode, decomp.substitutions.values()))
				){
				sMax = decomp.getReconstructedModel(element); 
			}
		}
		return sMax;
	}


	/**
	 * Returns the number of ctxUnit contained in a graph.
	 * N.b: size != number of nodes (GEO ctx unit and TIME count as one even if they are
	 * made of multiple nodes)
	 * @param node
	 * @return
	 */
	private long getSize(RDFNode node, Collection<String> RDFclasses) {
		
		Model model = node.getModel();
		if (model != null){
			ContextUnitConverter converter = new ContextUnitConverter();
			converter.convertInputToUnits(node, RDFclasses);
			return converter.inputGraphContextUnits.size();
		}
		else
			// is ctxUnit
			return 1;
	}



	private RDFNode difference(RDFNode g, RDFNode sMax, Edge e) {
		RDFNode gMinusSmax = null;
		Model gM = g.getModel();
		Model sMaxM = sMax.getModel();
		Statement cutStmt = ResourceFactory.createStatement(e.v1.instance.asResource(), e.label, e.v2.instance);
		
		
		// if sMax ctxUnit
		if (sMaxM == null || sMaxM.isEmpty()){
			Model mCopy = ModelFactory.createDefaultModel();
			mCopy.add(gM.listStatements());
			mCopy.remove(cutStmt);
			// if gminusSmax is supposed to be a ctxunit
			if (mCopy.isEmpty()){
				if (e.v2.instance.isResource())
					gMinusSmax = ResourceFactory.createResource(e.v2.instance.asResource().getURI());
				else if (e.v2.instance.isLiteral())
					gMinusSmax = ResourceFactory.createPlainLiteral(e.v2.instance.asLiteral().getString());
				return gMinusSmax;
			}
			
			// if smax is subject of Edge e
			if (e.v1.instance.equals(sMax)){
				if (e.v2.instance.isResource())
					gMinusSmax = ContextUnitConverter.getRoot(mCopy, true);
				else if (e.v2.instance.isLiteral())
					gMinusSmax = ContextUnitConverter.getRoot(mCopy, false);
			} 
			// Smax is object of Edge e
			// i.e. e.v2.instance.equals(sMax) == true
			else {
				gMinusSmax = ContextUnitConverter.getRoot(mCopy, true);
			}
			
			return gMinusSmax;
		} 
		
		
		
		// if smax not ctxunit
		else {
			Model gMinusSmaxM = gM.difference(sMaxM);
			
			// if gMinusSmax has only one triple, this triple is the cut triple.
			// Must check if this cut triple has siblings, and if so, must return the object of the statement.
			if (gMinusSmaxM.size() == 1){
				ResIterator it = gMinusSmaxM.listSubjects();
				while (it.hasNext()){
					Resource subj = it.next();
					if (subj.equals(cutStmt.getSubject()))
							gMinusSmax = cutStmt.getObject();
				}
			} else {
				gMinusSmaxM.remove(cutStmt);
				gMinusSmax = ContextUnitConverter.getRoot(gMinusSmaxM, true);
			}
		}
		return gMinusSmax;
	}




	
	/**
	 * Creates the RDFNode object that will consist in the first half of the decomposition process.
	 * @param e
	 * @param g
	 * @return
	 */
	private RDFNode createSMax(Edge e, RDFNode g) {
		
		Model gModel = g.getModel();
		
		// create a copy of model to not interfere
		Model mCopy = ModelFactory.createDefaultModel();
		mCopy.add(gModel.listStatements());
		
		// get cutprop form model to get complete branch
		// (cut property always present, so cutStmt never null)
		StmtIterator it = gModel.listStatements(e.v1.instance.asResource(), e.label, e.v2.instance);
		Statement cutStmt = null;
		if (it.hasNext())
			cutStmt = it.next();
		
		// remove statements outside the area of graph delimited by cutProperty
		removeStatements(mCopy, cutStmt.getObject(), null);
		
		RDFNode sMaxRDFNode = ContextUnitConverter.getRoot(mCopy, true); 
		if (ContextUnitConverter.isCtxUnit(sMaxRDFNode))
			return ContextUnitConverter.getRoot(mCopy, false);
		else{
			sMaxRDFNode.getModel().remove(cutStmt);
			return sMaxRDFNode;
		}
	}
	



	private void removeStatements(Model m, RDFNode node, Edge exception) {
		if (!node.isResource())
			return;
		Resource res = node.asResource();
		if (getPropertiesCount(res) == 0)
			return;
		
		Statement exceptionSt = null;
		if (exception!=null)
			exceptionSt = ResourceFactory.createStatement(exception.v1.instance.asResource(), 
					exception.label, 
					exception.v2.instance);
		StmtIterator it = res.listProperties();
		while(it.hasNext()){
			Statement s = it.next();
			if (exceptionSt == null || !exceptionSt.equals(s))
				removeStatements(m, s.getObject(),exception);
		}
		m.remove(res.listProperties());
		return;
	}


	private int getPropertiesCount(Resource res) {
		int count = 0;
		StmtIterator it = res.listProperties();
		while(it.hasNext()){
			it.next();
			count++;
		}
		return count;
	}





	/**
	 * Choose the edge between smax and the complementary graph.
	 * The edge would be added to the decomposition element.
	 * Priority given to core prissma properties.
	 * @param g
	 * @return
	 * @throws CuttingEdgeException 
	 */
	private Edge chooseCuttingEdge(RDFNode gnode, Collection<String> RDFclasses) throws CuttingEdgeException {
		
		Model g = gnode.getModel();
		
		StmtIterator iter = g.listStatements();
		Edge e;
		
		// Convert input g into ctx units.
		// Ctx unit conversion is needed to associate ctxUnit nodes to each Edge.
		ContextUnitConverter converter = new ContextUnitConverter();
		converter.convertInputToUnits(gnode, RDFclasses);
		
		// search for prissma core properties first. Pick one.
	    for (Property coreProperty : PrissmaProperties.priorityCutProperties) {
	    	if (g.contains( (Resource)null, coreProperty, (RDFNode)null)){
	    		e = converter.getEdge(coreProperty);
	    		return e;
	    	}
	    }
		
	    // if no prissma core property found, get the first returned property.
	    // Exclude internal properties of ctx units and PRISSMA core properties
		while(iter.hasNext()){
			Statement stmt = iter.nextStatement();
		    Property currentProperty = stmt.getPredicate();
		    if (!Arrays.asList(PrissmaProperties.internalCtxUnitProperties).contains(currentProperty) &&
		    	!isPriorityCutProp(currentProperty) ){
		    	e = converter.getEdge(currentProperty);
				return e;
		    }
		}
		
		
		
		// This means there were only filter properties in g. 
		// Should never happen, because ctx unit are never dissected.
		throw new CuttingEdgeException();
	}


	
	
	private boolean isPriorityCutProp(Property currentProperty) {
		List<Property> propList = Arrays.asList(PrissmaProperties.priorityCutProperties);
		if (propList.contains(currentProperty))
			return true;
		else
			return false;
	}



	private boolean isSubgraphOfTripleBased(RDFNode gElement, RDFNode g) {
		boolean result = false;
		Model gElementModel = gElement.getModel(); 
		// if decomp element is ctxUnit
		if (gElementModel == null || gElementModel.isEmpty()){
			result = g.getModel().containsResource(gElement);
			return result;
		}
		// if decompelement not ctxunit
		if (g.getModel().containsAll(gElement.getModel()))
			result = true;
		return result;
	}
	
	
}


