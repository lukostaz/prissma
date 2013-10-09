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

package fr.inria.wimmics.prissma.selection.utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import fr.inria.wimmics.prissma.selection.Decomposer;
import fr.inria.wimmics.prissma.selection.PrissmaProperties;
import fr.inria.wimmics.prissma.selection.entities.ContextUnit;
import fr.inria.wimmics.prissma.selection.entities.CtxUnitType;
import fr.inria.wimmics.prissma.selection.entities.Decomposition;
import fr.inria.wimmics.prissma.selection.entities.Edge;
import fr.inria.wimmics.prissma.selection.exceptions.InputConversionException;

public class ContextUnitConverter {
	
	
	public Set<ContextUnit> inputGraphContextUnits;
	public Set<Edge> inputGraphEdges;
	
	private  Logger LOG = LoggerFactory.getLogger(ContextUnitConverter.class);
	
	// put here to check if a ctxunit is of type CLASS
	public Map<String, String> substitutions;
	
	
	public ContextUnitConverter(){
		inputGraphContextUnits = new HashSet<ContextUnit>();
		inputGraphEdges = new HashSet<Edge>();
	}
	
	
	
	/**
	 * Modifies the input RDFNode by substituting all Resources w/ their RDFS/OWL class.
	 * Deletes the triples w/ property rdf:type from the model.
	 * 
	 * The function is used to compact the decomposition, since it improves the chances
	 * of collision, thus reducing fragmentation.
	 * @param decomp 
	 * 
	 * @param RDFNode node
	 */
	public static RDFNode switchToClasses(RDFNode node, Decomposition decomp) {
		
		//FIXME should check if entity has children, and if entity is not TIME nor GEO
		
		// detects all the rdf:type/a properties,
		// associate the class to the instance in an external map
		// and delete the triple.
		decomp.substitutions = new HashMap<String, String>();
		Model m = node.getModel();
		StmtIterator it = m.listStatements( 
				new SimpleSelector((Resource)null, PrissmaProperties.pType, (RDFNode) null) );
		while (it.hasNext()){
			Statement stmt = it.next();
			Resource subj = stmt.getSubject();
			Resource resClass = stmt.getObject().asResource();
			decomp.substitutions.put(subj.getURI(), resClass.getURI());
			it.remove();
		}
		
		// substitute each element of the map 
		// with its class everywhere in model
		for (String entityURI : decomp.substitutions.keySet()) {
			Resource classRes = ResourceFactory.createResource(decomp.substitutions.get(entityURI));
			// subjects first
			SimpleSelector selSubj = new SimpleSelector(ResourceFactory.createResource(entityURI), (Property) null, (RDFNode) null) ;
			it = m.listStatements(selSubj);
			while (it.hasNext()){
				Statement stmt = it.next();
				Property p = stmt.getPredicate();
				RDFNode obj = stmt.getObject();
				m.add(classRes, p, obj);
			}
			m.remove(m.listStatements(selSubj));
			// objects
			SimpleSelector selObj = new SimpleSelector((Resource) null, (Property) null, ResourceFactory.createResource(entityURI)) ;
			it = m.listStatements(selObj);
			while (it.hasNext()){
				Statement stmt = it.next();
				Property p = stmt.getPredicate();
				Resource subj = stmt.getSubject();
				m.add(subj, p, classRes);
			}
			m.remove(m.listStatements(selObj));
			
		}
		return ContextUnitConverter.getRootCtxNode(m);
	}

	
	
	
	/**
	 * Utility function.
	 * Sets inputGraphContextUnits and inputGraphEdges.
	 * @param gnode
	 * @throws InputConversionException 
	 */
	public void convertInputToUnits(RDFNode gnode,  Collection<String> collection) {
		
		
		if (gnode == null){
			LOG.info("input Ctx null. Assume test mode on. Skipping conversion.");
			return;
		}
		
		Model gNodeM = gnode.getModel();
		Edge geoEdge = null, timeEdge = null;
		
		// it is already a simple ctxUnit
		if (gNodeM == null || gNodeM.isEmpty() ){
			ContextUnit cu = null;
			if (gnode.isResource()){
				if (collection.contains(gnode.asResource().getURI()))
					cu = new ContextUnit(CtxUnitType.CLASS);
				else
					cu = new ContextUnit(CtxUnitType.ENTITY);
			}
			else if (gnode.isLiteral())
				cu = new ContextUnit(CtxUnitType.STRING);
			// TODO other datatypes
			cu.instance = gnode;
			inputGraphContextUnits.add(cu);
			return;
		} 
		// contains complex ctxunit (GEO)
		else if (containsOnlyInternalProp(gnode, PrissmaProperties.internalGEOProperties)){
			ContextUnit cu = new ContextUnit(CtxUnitType.GEO);
			cu.instance = gnode;
			inputGraphContextUnits.add(cu);
		} 		
		// contains complex ctxunit (TIME )
		else if (containsOnlyInternalProp(gnode, PrissmaProperties.internalTIMEProperties)){
			ContextUnit cu = new ContextUnit(CtxUnitType.TIME);
			cu.instance = gnode;
			inputGraphContextUnits.add(cu);
		}
		
		// if it's not a ctxUnit
		StmtIterator iter = gNodeM.listStatements();
		Edge edge;
		
		// Storage for complex ctxUnits (GEO, TIME)
		RDFNode lat = null, lon = null, radius = null, start = null, duration = null, poi = null, time = null;
		ContextUnit cuGeo = null, cuTime = null;
		
		while (iter.hasNext()){
			ContextUnit v2=null, v1=null;
			Statement stmt = iter.nextStatement();
			Resource  subject   = stmt.getSubject();    
		    Property  predicate = stmt.getPredicate();  
		    RDFNode   object    = stmt.getObject();      
		    
			// check for GEO ctx unit type
			if (predicate.equals(PrissmaProperties.pPOI)){
				v2 = new ContextUnit(CtxUnitType.GEO);
				cuGeo = v2;
			}
			// check for TIME ctx unit type	
			else if (predicate.equals(PrissmaProperties.pTime)){
				v2 = new ContextUnit(CtxUnitType.TIME);	
				cuTime = v2;
			}
			else if (!Arrays.asList(PrissmaProperties.internalCtxUnitProperties).contains(predicate)){
				// check if ENTITY type
				if (object.isResource()){
					if (collection.contains(object.asResource().getURI()))
						v2 = new ContextUnit(CtxUnitType.CLASS);
					else
						v2 = new ContextUnit(CtxUnitType.ENTITY);
				}
				// check if STRING type //FIXME literal!=string
				else if (object.isLiteral())
					v2 = new ContextUnit(CtxUnitType.STRING);
			} else{
				if (predicate.equals(PrissmaProperties.pLat))
					lat = object;
				else if (predicate.equals(PrissmaProperties.pLon))
					lon = object;
				else if (predicate.equals(PrissmaProperties.pRad))
					radius = object;
				else if (predicate.equals(PrissmaProperties.pStart))
					start = object;
				else if (predicate.equals(PrissmaProperties.pDuration))
					duration = object;
			}
			
			
			if (v2 != null) {
				
				// v1 is always ENTITY or CLASS type by construction
				if (collection.contains(subject.asResource().getURI()))
					v1 = new ContextUnit(CtxUnitType.CLASS);
				else
					v1 = new ContextUnit(CtxUnitType.ENTITY);
				
				v1.instance = subject;
				inputGraphContextUnits.add(v1); // duplicates are discarded
				
				if (v2.type != CtxUnitType.GEO && v2.type != CtxUnitType.TIME) {
					v2.instance = object;
					inputGraphContextUnits.add(v2); // duplicates are discarded
				} 
				else if (v2.type == CtxUnitType.GEO)
					poi = object;
				else if (v2.type == CtxUnitType.TIME)
					time = object;
				
				// populate Edge and add it to list
				edge = new Edge();
				ContextUnit v1Edge = new ContextUnit(v1.type);
				ContextUnit v2Edge = new ContextUnit(v2.type);
				Resource v1EdgeInstance = ResourceFactory.createResource(v1.instance.asResource().getURI());
				// if v2 ctxunit
				if (v2.instance == null)
					edge.v2 = v2;
				else{
					if (v2.instance.isResource())
						v2Edge.instance = ResourceFactory.createResource(v2.instance.asResource().getURI());
					else if (v2.instance.isLiteral())
						v2Edge.instance = ResourceFactory.createPlainLiteral(v2.instance.asLiteral().getString());
					edge.v2 = v2Edge;
				}	
				v1Edge.instance = v1EdgeInstance;
				edge.v1 = v1Edge;
				edge.label = predicate;
				if (v2.type == CtxUnitType.GEO)
					geoEdge = edge;
				if (v2.type == CtxUnitType.TIME)
					timeEdge = edge;
				if (v2.type != CtxUnitType.GEO && v2.type != CtxUnitType.TIME)
					inputGraphEdges.add(edge);
			}
			
		}
		
		// Add GEO and TIME, if any.
		if (cuGeo != null){
			Model m = ModelFactory.createDefaultModel();
			if (lat != null)
				m.add(poi.asResource(), PrissmaProperties.pLat, lat);
			if (lon != null)
				m.add(poi.asResource(), PrissmaProperties.pLon, lon);
			if (radius != null)
				m.add(poi.asResource(), PrissmaProperties.pRad, radius);
			cuGeo.instance = getRoot(m, true);
			geoEdge.v2 = cuGeo;
			inputGraphEdges.add(geoEdge);
			inputGraphContextUnits.add(cuGeo);
			
		} 
		if (cuTime != null) {
			Model m = ModelFactory.createDefaultModel();
			if (start != null)
				m.add(time.asResource(), PrissmaProperties.pStart, start);
			if (duration != null)
				m.add(time.asResource(), PrissmaProperties.pDuration, duration);
			cuTime.instance = getRoot(m, true);
			timeEdge.v2 = cuTime;
			inputGraphEdges.add(timeEdge);
			inputGraphContextUnits.add(cuTime);
			
		}
		
		return;
	}


	private static boolean containsOnlyInternalProp(RDFNode g, Property[] properties) {
		
		Model mg = g.getModel();
		if (mg == null)
			return false;
		
		// check geo and time
		StmtIterator it = mg.listStatements();
		while (it.hasNext()){
			Statement st = it.next();
			Property p = st.getPredicate();
			if (!Arrays.asList(properties).contains(p))
				return false;
		}
		return true; // it contains only internalCtxUnitProperties (--> GEO or TIME ctxUnit)
	}


	/**
	 * Return converted edge according to the desired jena Property.
	 * @param prop
	 * @return
	 */
	public Edge getEdge(Property prop) {
		
		Iterator<Edge> it =  this.inputGraphEdges.iterator();
		while(it.hasNext()){
			Edge currentEdge = it.next();
			if (currentEdge.label.equals(prop))
				return currentEdge;
		}
		
		return null;
	}
	
	
	/**
	 * Gets the root of a model (tree) and returns it in RDFNode object.
	 * @param m
	 * @param withProperties 
	 * @return
	 */
	public static RDFNode getRoot(Model m, boolean withProperties) {
		
		List<RDFNode> subjects = new ArrayList<RDFNode>();
		List<RDFNode> objects = new ArrayList<RDFNode>();
		
		StmtIterator it = m.listStatements();
		while (it.hasNext()){
			Statement st = it.next();
			subjects.add(st.getSubject());
			objects.add(st.getObject());
		}
		
		for (RDFNode currSub : subjects) {
			if (!objects.contains(currSub)){
				if (withProperties)
					return currSub;
				else{
					RDFNode cleanCopy = createCleanCopy(currSub);
					return cleanCopy;
				}
			}
		}
		return null;
	}


	/**
	 *  create a copy of the RDFNode stripped out of the underlying model.
	 * @param currSub
	 * @return
	 */
	private static RDFNode createCleanCopy(RDFNode currSub) {
		RDFNode copy = null;
		if (currSub.isResource()){
			copy = ResourceFactory.createResource(currSub.asResource().getURI());
		} else if (currSub.isLiteral()){
			copy = ResourceFactory.createPlainLiteral(currSub.asLiteral().getString());
		}
		return copy;
	}
	
	

	public static boolean isCtxUnit(List<RDFNode> gList) {
		
		if (gList.size() > 1)
			return false;
		
		for (RDFNode g : gList) {
			if (! g.isLiteral() && !containsOnlyInternalProp(g, PrissmaProperties.internalCtxUnitProperties))
				return false;
		}
		return true;
	}
	
	public static boolean isCtxUnit(RDFNode g) {
		if ( g.isLiteral() || 
			isResourceLeaf(g) ||
			containsOnlyInternalProp(g, PrissmaProperties.internalCtxUnitProperties)
			)
			return true;
		return false;
	}
	
	
	private static boolean isResourceLeaf(RDFNode g) {
		if (g.isResource()){
			
			if (g.getModel() == null)
				return true;
			
			StmtIterator it = g.asResource().listProperties();
			while (it.hasNext())
				return false;
		}
		return true;
	}



	public static boolean areIsomorphic(List<RDFNode> mItem, List<RDFNode> rootNodes) {
		
		int foundCnt = 0;
		
		for (RDFNode mItemNode : mItem) {
			for (RDFNode rdfNode : rootNodes) {
				if (areIsomorphic(mItemNode, rdfNode))
					foundCnt ++;
			}
		}
		if (foundCnt == mItem.size())
			return true;
		else
			return false;
		
	}
	
	public static boolean areIsomorphic(RDFNode g, RDFNode sMax) {
//		
//		if (gList.size() > 1)
//			return false;
		
//		for (RDFNode g : gList) {
			if (sMax == null)
				return false;
			Model mG = g.getModel();
			Model mSMax = sMax.getModel();
			
			// put empty models to null for simpler if clauses
			if (mG != null && mG.isEmpty())
				mG = null;
			if (mSMax != null && mSMax.isEmpty())
				mSMax = null;
			
			
			// both ctxUnit
			if ( (mG == null && mSMax == null) ){
				if (!g.equals(sMax))
					return false;
			// one of them is ctxunit
			} else if (mG != null && mSMax == null){
				return false;
			} else if (mG == null && mSMax != null){
				return false;
			} 
			// not ctx units
			else{
				if (!mG.getGraph().isIsomorphicWith(mSMax.getGraph()))
					return false;
			}
//		}
		return true;
		
	}
	
	
	
	
	public static RDFNode getRootCtxNode(Model prismModel) {
		RDFNode ctx = getSubjectForCoreProp(prismModel, PrissmaProperties.pUsr);
		if ( ctx != null)
			return ctx;
		ctx = getSubjectForCoreProp(prismModel, PrissmaProperties.pDev);
		if ( ctx != null)
			return ctx;
		ctx = getSubjectForCoreProp(prismModel, PrissmaProperties.pEnv);
		if ( ctx != null)
			return ctx;
		
		return null;
	}

	private static RDFNode getSubjectForCoreProp(Model prismModel, Property pCore) {
		ResIterator it = prismModel.listResourcesWithProperty(pCore);
		RDFNode ctx =null;
		while (it.hasNext()){
			ctx = it.next();
		}
		return ctx;
	}



	
	
}
