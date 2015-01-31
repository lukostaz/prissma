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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;






import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Jaro;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;

import com.hp.hpl.jena.rdf.model.RDFNode;

import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.lexical_db.data.Concept;
import edu.cmu.lti.ws4j.Relatedness;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import fr.inria.wimmics.prissma.selection.entities.ContextUnit;
import fr.inria.wimmics.prissma.selection.entities.CtxUnitType;
import fr.inria.wimmics.prissma.selection.entities.DecompItem;
import fr.inria.wimmics.prissma.selection.entities.Decomposition;
import fr.inria.wimmics.prissma.selection.entities.ETSubgraphIsomorphism;
import fr.inria.wimmics.prissma.selection.entities.Edge;
import fr.inria.wimmics.prissma.selection.entities.EditOperation;
import fr.inria.wimmics.prissma.selection.entities.EditOperationType;
import fr.inria.wimmics.prissma.selection.entities.StringSimilarity;
import fr.inria.wimmics.prissma.selection.exceptions.ContextUnitException;
import fr.inria.wimmics.prissma.selection.utilities.ContextUnitConverter;

public class Matcher {

	public Decomposition decomp;	
	public Set<URI> results;
	
	public Map<Integer,List<ETSubgraphIsomorphism>> candidates;
	public Map<Integer,List<ETSubgraphIsomorphism>> winners;
	
	
	public Set<ContextUnit> inputGraphContextUnits;
	public Set<Edge> inputGraphEdges;
	
	private double currentMinCost;
	
	
	/** Semantic Similarity for Strings*/
	private static ILexicalDatabase db = new NictWordNet();
    private static RelatednessCalculator lin = new Lin(db);
    private static RelatednessCalculator wup = new WuPalmer(db);
    private static RelatednessCalculator path = new Path(db);
	
	
	
	private  Logger LOG = LoggerFactory.getLogger(Matcher.class);
	
	
	public Matcher(Decomposition decomp) {
		if (decomp == null)
			this.decomp = new Decomposition();
		else
			this.decomp = decomp;
		this.inputGraphContextUnits = new HashSet<ContextUnit>();
		this.inputGraphEdges = new HashSet<Edge>();
		this.candidates = new HashMap<Integer, List<ETSubgraphIsomorphism>>();
		this.winners = new HashMap<Integer, List<ETSubgraphIsomorphism>>();
		this.results = new HashSet<URI>();
		
		// read params from property file
		try {
			Configuration config = new PropertiesConfiguration("config.properties");
			PrissmaProperties.THRESHOLD = config.getDouble("threshold");
			PrissmaProperties.MISSING_CTXUNIT_ENTITY_COST = config.getDouble("missing_ctxunit_entity_cost");
			PrissmaProperties.MISSING_CTXUNIT_STRING_COST = config.getDouble("missing_ctxunit_string_cost");
			PrissmaProperties.DECAY_CONSTANT_TIME = config.getDouble("decay_constant_time");
			PrissmaProperties.DECAY_CONSTANT_GEO = config.getDouble("decay_constant_geo");
			
			switch (config.getString("string_similarity")) {
			case "JARO":
				PrissmaProperties.STRING_SIMILARITY = StringSimilarity.JARO; 
				break;
			case "JARO_WINKLER":
				PrissmaProperties.STRING_SIMILARITY = StringSimilarity.JARO_WINKLER; 
				break;	
			case "MONGE_ELKAN":
				PrissmaProperties.STRING_SIMILARITY = StringSimilarity.MONGE_ELKAN; 
				break;
			case "LEVENSTHEIN":
				PrissmaProperties.STRING_SIMILARITY = StringSimilarity.LEVENSTHEIN; 
				break;
			case "LIN":
				PrissmaProperties.STRING_SIMILARITY = StringSimilarity.LIN; 
				break;
			case "WUPALMER":
				PrissmaProperties.STRING_SIMILARITY = StringSimilarity.WUPALMER; 
				break;
			case "PATH":
				PrissmaProperties.STRING_SIMILARITY = StringSimilarity.PATH; 
				break;
			default:
				LOG.error("Similarity measure not supported");
				break;
		}
			
			
			
			PrissmaProperties.ENTITIES_PATH = config.getString("fresnel_folder");
		} catch (ConfigurationException e) {
			LOG.error("Error reading property file {}", e.getMessage());
		}
	
		
		
	}



	public void search(RDFNode inputCtx){
		
		// preliminary: create input graph context units
		// sets inputGraphContextUnits and inputGraphEdges.
		ContextUnitConverter c = new ContextUnitConverter();
		c.convertInputToUnits(inputCtx, decomp.substitutions.values());
		this.inputGraphContextUnits = c.inputGraphContextUnits;
		this.inputGraphEdges = c.inputGraphEdges;
		
		
		// First, compute et-subgraph isomorphism from each context unit to decomposition elements.
		// (match each context unit in the decomposition with input graph
		// and computes context unit costs.)
		for (DecompItem item : decomp.elements){ 
			if (item.isCtxUnit){
				candidates.put(Integer.valueOf(item.id), matchDecompCtxUnitToInputGraph(item));
			}
		}
		
		// returns element in D with et-sub-is with lowest cost
		DecompItem item1 = getDecompMin();
		
		while(item1 != null && this.currentMinCost <= PrissmaProperties.THRESHOLD){
			LOG.info("Min Item:" + item1);
			// use first element of each candidate list since candidate has been sorted in getDecompMin()
			ETSubgraphIsomorphism f1 = candidates.get(item1.id).remove(0);
			if (winners.get(item1.id)==null)
				winners.put(item1.id, new ArrayList<ETSubgraphIsomorphism>());
			winners.get(item1.id).add(f1);
			
			// if it is a prism, add to results
			if (item1.prismURISet != null && !item1.prismURISet.isEmpty()){
//				if (computeCost(item1) <= PrissmaProperties.THRESHOLD )
					results.addAll(item1.prismURISet);
			}
			
			// search all descendants of item1
			for (DecompItem item : decomp.elements){
				DecompItem item2 = null;
				// determines if item is descendant of item1
				if (item.idAncestor1 == item1.id)
					item2 = decomp.elements.get(item.idAncestor2);
				else if(item.idAncestor2 == item1.id)
					item2 = decomp.elements.get(item.idAncestor1);
				
				// if the descendant item exists and there is another ancestor item2
				if (item2!=null){
					// pick elements from winner of the other ancestor
					if(winners.get(item2.id)!=null){
						for(ETSubgraphIsomorphism f2 : winners.get(item2.id)){
							ETSubgraphIsomorphism f = combine(item.edges,f1,f2); 
							// add f to candidates(item.id)
							if (f!=null){
								if (candidates.get(item.id)==null)
									candidates.put(item.id, new ArrayList<ETSubgraphIsomorphism>());
								candidates.get(item.id).add(f);
							}
								
						}
					}
				}
			}
			// set next element
			item1 = getDecompMin();
		}
		return;
	}
	
	
	
	private double computeCost(DecompItem item) {
		double cost = 0;
		List<ETSubgraphIsomorphism> winners = this.winners.get(item.id);
		ETSubgraphIsomorphism etSubgraphIsomorphism = winners.get(0);
		for (EditOperation op : etSubgraphIsomorphism.deltaList) {
			cost += op.cost;
		}
		cost = cost/etSubgraphIsomorphism.deltaList.size();
		return cost;
	}



	/**
	 * Adapted Messmer&Bunke combine support function. 
	 */
	private ETSubgraphIsomorphism combine(List<Edge> edges,
			ETSubgraphIsomorphism f1, ETSubgraphIsomorphism f2){
		
		ETSubgraphIsomorphism f = new ETSubgraphIsomorphism();
		
		//TODO check images of f1 + f2 to avoid duplicates in merge ancestors
		
		// Merge ancestors' graph edit operations (deltas)
		f.deltaList.addAll(f1.deltaList);
		f.deltaList.addAll(f2.deltaList);
		
		// build delta_e
		for (Edge edge : edges) {
			EditOperation edgeOp =null;
			Iterator<Edge> itInputEdges = inputGraphEdges.iterator();
			boolean inputFound = false;
			while(itInputEdges.hasNext() && !inputFound){
				Edge eInput = itInputEdges.next();
				// 2) edge substitution, i.e. if the edge exists
				if (eInput.v1.equals(edge.v1) && eInput.v2.equals(edge.v2)){
					edgeOp = substituteEdge(eInput,edge);
					inputFound = true;
				}
			}
			// 3) edge deletion, i.e. edge does not exist in input graph
			if (!inputFound){
				edgeOp = deleteEdge(edge); //TODO
			}
			
			if (edgeOp!=null)
				f.deltaList.add(edgeOp);
		}
		
//		// 1) edge insertion, i.e. edge in input but not in decomposition //TODO
//		for (Edge inputEdge : inputGraphEdges) {
//			Iterator<Edge> itdecompEdges = edges.iterator();
//			boolean decompFound = false;
//		}
//		edgeOp = insertEdge(edge); // TODO
		
		f.cost = (f1.cost + f2.cost)/2;
		
		return f;
	}
	
	
	
	
	private EditOperation insertEdge(Edge edge) {
		// TODO Auto-generated method stub
		return null;
	}



	private EditOperation deleteEdge(Edge inputEdgeToBeFound) {
		// TODO Auto-generated method stub
		return null;
	}



	/**
	 * Computes edit operation of substituting an edge w/ another.
	 * //FIXME For the time being the cost of the operation is set to minumum
	 * (we assume that the edge to be substituted is the same). 
	 * @param eInput
	 * @param inputEdgeToBeFound
	 * @return
	 */
	private EditOperation substituteEdge(Edge eInput,
			Edge inputEdgeToBeFound) {
		EditOperation edgeOp = new EditOperation();
		edgeOp.type = EditOperationType.SUB_PROP;
		edgeOp.cost = PrissmaProperties.MIN; // FIXME this is temporary, as only perfectly equality is supported for edges so far.
		edgeOp.edgeDecomp = inputEdgeToBeFound;
		edgeOp.edgeInput = eInput;
		return edgeOp;
	}


	/**
	 * Finds and returns decompItem with minimum cost and sets current global minimum cost.
	 * 
	 * @return the item in the decomposition with minimum cost
	 */
	private DecompItem getDecompMin() {
		
		if (this.candidates==null || this.candidates.isEmpty())
			return null;
		
		Integer minItemID = null;
		// set to maximum cost
		double minItemCost = PrissmaProperties.MAX;
		
		Iterator<Entry<Integer, List<ETSubgraphIsomorphism>>> it = this.candidates.entrySet().iterator();
		while (it.hasNext()){
			Map.Entry<Integer, List<ETSubgraphIsomorphism>> pairs = it.next();
			List<ETSubgraphIsomorphism> isoListNode = (List<ETSubgraphIsomorphism>) pairs.getValue();
			if (!isoListNode.isEmpty()) {
				Collections.sort(isoListNode);
				double itemCost = isoListNode.get(0).cost;
				Integer itemID = (Integer) pairs.getKey();
				if (itemCost < minItemCost) {
					minItemCost = itemCost;
					minItemID = itemID;
				}
			}
		}
		
		// if not found
		if (minItemID==null)
			return null;
		
		this.currentMinCost = minItemCost; // set minimum cost globally
		return decomp.elements.get(minItemID);
	}



	
	/**
	 * Context Unit Matching method
	 * @param decompUnit
	 * @return
	 */
	private List<ETSubgraphIsomorphism> matchDecompCtxUnitToInputGraph(DecompItem decompUnit){
		
		List<ETSubgraphIsomorphism> ETSIList = new ArrayList<ETSubgraphIsomorphism>();
		for (ContextUnit inputUnit : this.inputGraphContextUnits) {
			ContextUnit instanceCtxUnit = decompUnit.ctxUnit;
			ETSubgraphIsomorphism substitution = null;
			switch (instanceCtxUnit.type) {
			case GEO: 
				substitution = computeNodeSubstitutionGeo(instanceCtxUnit, inputUnit);
				break;
			case TIME:
				substitution = computeNodeSubstitutionTime(instanceCtxUnit, inputUnit);
				break;
			case STRING:
				substitution = computeNodeSubstitutionString(instanceCtxUnit, inputUnit);
				break;
			case ENTITY:
				substitution = computeNodeSubstitutionEntity(instanceCtxUnit, inputUnit);
				break;
			case CLASS:
				substitution = computeNodeSubstitutionClass(instanceCtxUnit, inputUnit);
				break;
			default:
				break;
			}
			if (substitution!=null)
				ETSIList.add(substitution);
		}
		
		// add deletion
		ETSubgraphIsomorphism deletion = null;
		deletion = computeNodeDeletion(decompUnit.ctxUnit);
		if (deletion!=null)
			ETSIList.add(deletion);
		
		Collections.sort(ETSIList); // FIXME, remove from here and put in computeCost()?
		return ETSIList;
	}
	
	
	

	/**
	 * Computes node substitution for Temporal nodes.
	 * @param instanceCtxUnit
	 * @param inputUnit
	 * @return
	 */
	private ETSubgraphIsomorphism computeNodeSubstitutionTime(
			ContextUnit decompCtxUnit, ContextUnit inputCtxUnit) {
		
		EditOperation op =  new EditOperation();
		ETSubgraphIsomorphism isosub = new ETSubgraphIsomorphism();
		
		// check if right context unit input type
		if (inputCtxUnit.type != CtxUnitType.TIME ){
			op.cost = PrissmaProperties.MAX;
		} else {
			double inStart, decompDuration, decompStart;
			
			try {
				inStart = inputCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pStart);
				decompDuration = decompCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pDuration);
				decompStart = decompCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pStart);

				// Exponential decay
				// first, check if input time is in duration
				double exceeedingtime = inStart - decompStart - decompDuration;
				// normalize exceeding time on declared duration. 
				// Needed to have significant impact of the exponential decay.
				double exceeedingtimePerc = exceeedingtime / decompDuration;
				if (inStart >= decompStart && exceeedingtime <= 0)
					op.cost = PrissmaProperties.MIN;
				else if (inStart > decompStart && exceeedingtime > 0) {
					op.cost = 1 - Math.exp(- PrissmaProperties.DECAY_CONSTANT_TIME * exceeedingtimePerc);
				}
				else if (inStart < decompStart){
					exceeedingtime = decompStart - inStart;
					exceeedingtimePerc = exceeedingtime / decompDuration;
					op.cost = 1 - Math.exp(- PrissmaProperties.DECAY_CONSTANT_TIME * exceeedingtimePerc);
				}
				
			} catch (ContextUnitException e) {
				// if time ctxunit not complete, raise to max cost
				op.cost = PrissmaProperties.MAX;
			}
		}
		
		op.type = EditOperationType.SUB_ENT;
		op.gDecomp = decompCtxUnit;
		op.gInput = inputCtxUnit;
		isosub.deltaList.add(op);
		// update cost
		isosub.cost = (isosub.cost + op.cost)/isosub.deltaList.size();
		return isosub;
	}




	/**
	 * Computes delete node edit operation. 
	 * The computed cost represents the cost of having a missing ContextUnit in 
	 * the input graph.
	 * 
	 * @param instanceCtxUnit
	 * @return
	 */
	private ETSubgraphIsomorphism computeNodeDeletion(ContextUnit instanceCtxUnit) {

		ETSubgraphIsomorphism deletion = new ETSubgraphIsomorphism();
		EditOperation opDel =  new EditOperation();
		opDel.type = EditOperationType.DEL_ENT;
		opDel.gDecomp = instanceCtxUnit;
		opDel.gInput = null;
		
		/* Assign appropriate cost to deletion:
		   - Missing GEO or TIME ctxUnit: maximum cost	
		   - Missing ENTITY or STRING ctxUnit w/ no properties: pre-set cost
		   - Missing CLASS ctxUnit: maximum cost 
		*/	
		switch (instanceCtxUnit.type) {
		case GEO:
			opDel.cost = PrissmaProperties.MAX;
			break;
		case TIME:
			opDel.cost = PrissmaProperties.MAX;
			break;
		case ENTITY:
			opDel.cost = PrissmaProperties.MISSING_CTXUNIT_ENTITY_COST;
			break;
		case STRING:
			opDel.cost = PrissmaProperties.MISSING_CTXUNIT_STRING_COST;
			break;
		case CLASS:
			opDel.cost = PrissmaProperties.MAX;
			break;
		default:
			break;
		}
		
		deletion.deltaList.add(opDel);
		// update ETSubgraphIsomorphism cost
		deletion.cost = (deletion.cost + opDel.cost)/deletion.deltaList.size();
		return deletion;
	}


	/**
	 * Compute cost of substitution for a geo context unit
	 * @param instances
	 * @param inputLocation
	 * @return
	 */
	private ETSubgraphIsomorphism computeNodeSubstitutionGeo(ContextUnit decompCtxUnit,
			ContextUnit inputCtxUnit) {
		
		EditOperation op =  new EditOperation();
		ETSubgraphIsomorphism isosub = new ETSubgraphIsomorphism();
		
		// check if right context unit input type
		if (inputCtxUnit.type != CtxUnitType.GEO ){
			op.cost = PrissmaProperties.MAX;
		} else {
			double latRef, lonRef, latIn, lonIn, dist, radiusRef;
			
			try {
				latIn = inputCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pLat);
				lonIn = inputCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pLon);
				latRef = decompCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pLat);
				lonRef = decompCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pLon);
				radiusRef = decompCtxUnit.getComplexCtxUnitProp(PrissmaProperties.pRad) / 1000; // Km
				
				dist = haversineDistance(latIn, lonIn, latRef, lonRef); // Km
				// if input location inside p:radius
				if (dist <= radiusRef)
					op.cost = PrissmaProperties.MIN;
				// if outside radius, compute half-normal probability
				else{
					// exponential decay
					double edgeDist = dist - radiusRef;
					// need to normalize the distance from circle
					double edgeDistPerc = edgeDist / dist;
					op.cost = 1 - Math.exp(- PrissmaProperties.DECAY_CONSTANT_GEO * edgeDistPerc);
				}
				
			} catch (ContextUnitException e) {
				// if at least one geo element not present, raise to max cost
				op.cost = PrissmaProperties.MAX;
			}
		}
		
		op.type = EditOperationType.SUB_ENT;
		op.gDecomp = decompCtxUnit;
		op.gInput = inputCtxUnit;
		isosub.deltaList.add(op);
		// update cost
		isosub.cost = (isosub.cost + op.cost)/isosub.deltaList.size();
		return isosub;
	}


	/**
	 * Computes edit operation of substituting a entity-type node.
	 * @param decompCtxUnit
	 * @param inputUnit
	 * @return
	 */
	private ETSubgraphIsomorphism computeNodeSubstitutionClass(ContextUnit decompCtxUnit,
		ContextUnit inputUnit) {
	
		ETSubgraphIsomorphism isosub = new ETSubgraphIsomorphism();
		EditOperation op =  new EditOperation();
		// check if right context unit input type
		if (inputUnit.type != CtxUnitType.CLASS){
			op.cost = 1;
		} else {
			// get first and only instance because it is instance type
			RDFNode inputNode = inputUnit.instance;
			RDFNode decompNode = decompCtxUnit.instance;
			// check if URIs are equivalent
			if(inputNode.asResource().getURI().equals(decompNode.asResource().getURI())){
				op.cost = PrissmaProperties.MIN;
			} else{
				op.cost = PrissmaProperties.MAX;
			}
		}
		
		op.type = EditOperationType.SUB_ENT;
		op.gDecomp = decompCtxUnit;
		op.gInput = inputUnit;
		isosub.deltaList.add(op);
		// update cost
		isosub.cost = (isosub.cost + op.cost)/isosub.deltaList.size();
		
		return isosub;
	}

	/**
	 * Computes edit operation of substituting a entity-type node.
	 * @param decompCtxUnit
	 * @param inputUnit
	 * @return
	 */
	private ETSubgraphIsomorphism computeNodeSubstitutionEntity(ContextUnit decompCtxUnit,
		ContextUnit inputUnit) {
	
		ETSubgraphIsomorphism isosub = new ETSubgraphIsomorphism();
		EditOperation op =  new EditOperation();
		// check if right context unit input type
		if (inputUnit.type != CtxUnitType.ENTITY){
			op.cost = 1;
		} else {
			// get first and only instance because it is instance type
			RDFNode inputNode = inputUnit.instance;
			RDFNode decompNode = decompCtxUnit.instance;
			// check if URIs are equivalent
			if(inputNode.asResource().getURI().equals(decompNode.asResource().getURI())){
				op.cost = PrissmaProperties.MIN;
			} else{
				op.cost = PrissmaProperties.MAX;
			}
		}
		
		op.type = EditOperationType.SUB_ENT;
		op.gDecomp = decompCtxUnit;
		op.gInput = inputUnit;
		isosub.deltaList.add(op);
		// update cost
		isosub.cost = (isosub.cost + op.cost)/isosub.deltaList.size();
		
		return isosub;
	}

	
	/**
	 * Computes the edit operation of substituting a string type node.
	 * @param decompCtxUnit
	 * @param inputUnit
	 * @return
	 */
	private ETSubgraphIsomorphism computeNodeSubstitutionString(ContextUnit decompCtxUnit,
			ContextUnit inputUnit) {
		
			ETSubgraphIsomorphism isosub = new ETSubgraphIsomorphism();
			EditOperation op =  new EditOperation();
			// check if right context unit input type
			if (inputUnit.type != CtxUnitType.STRING ){
				op.cost = PrissmaProperties.MAX;
			} else {
				// get first and only element because it is string type
				RDFNode inputNode = inputUnit.instance;
				RDFNode decompNode = decompCtxUnit.instance;
				// string matching
				String inputStr = inputNode.asNode().getLiteral().getValue().toString();
				String decompString = decompNode.asNode().getLiteral().getValue().toString();
				switch (PrissmaProperties.STRING_SIMILARITY) {
					case JARO:
						op.cost = PrissmaProperties.MAX - jaroSimilarity(inputStr,decompString); 
						break;
					case JARO_WINKLER:
						op.cost = PrissmaProperties.MAX - jaroWinklerSimilarity(inputStr,decompString); 
						break;	
					case MONGE_ELKAN:
						op.cost = PrissmaProperties.MAX - mongeElkanSimilarity(inputStr, decompString); 
						break;
					case LEVENSTHEIN:
						op.cost = PrissmaProperties.MAX - levenstheinSimilarity(inputStr, decompString); 
						break;
					case LIN:
						op.cost = PrissmaProperties.MAX - 
							semanticStringSimilarity(inputStr, decompString, StringSimilarity.LIN); 
						break;
					case WUPALMER:
						op.cost = PrissmaProperties.MAX - 
							semanticStringSimilarity(inputStr, decompString, StringSimilarity.WUPALMER); 
						break;
					case PATH:
						op.cost = PrissmaProperties.MAX - 
							semanticStringSimilarity(inputStr, decompString, StringSimilarity.PATH); 
						break;
					default:
						LOG.error("Similarity measure not supported");
						op.cost = 1;
						break;
				}
				
				
			}
			
			op.type = EditOperationType.SUB_ENT;
			op.gDecomp = decompCtxUnit;
			op.gInput = inputUnit;
			isosub.deltaList.add(op);
			// update cost
			isosub.cost = (isosub.cost + op.cost)/isosub.deltaList.size();
			
			return isosub;
		}



	private double semanticStringSimilarity(String inputStr,
			String decompString, StringSimilarity method) {

		WS4JConfiguration.getInstance().setMFS(true);
		
		RelatednessCalculator rc = null;
		switch (PrissmaProperties.STRING_SIMILARITY) {
			case LIN:
				rc = lin;
				break;
			case WUPALMER:
				rc = wup;
				break;
			case PATH:
				rc = path;
				break;
			default:
				LOG.error("Similarity measure not supported");
				return -1;
		}
		List<POS[]> posPairs = rc.getPOSPairs();
		double maxScore = -1D;

		for (POS[] posPair : posPairs) {
			List<Concept> synsets1 = (List<Concept>) db.getAllConcepts(inputStr,
					posPair[0].toString());
			List<Concept> synsets2 = (List<Concept>) db.getAllConcepts(decompString,
					posPair[1].toString());

			for (Concept synset1 : synsets1) {
				for (Concept synset2 : synsets2) {
					Relatedness relatedness = rc.calcRelatednessOfSynset(
							synset1, synset2);
					double score = relatedness.getScore();
					if (score > maxScore) {
						maxScore = score;
					}
				}
			}
		}

		if (maxScore == -1D) {
			maxScore = 0.0;
		}

		return maxScore;
	}



	/**
	 * Computes monge-elkan similarity between two strings.
	 * Used to compute similarity between String-type literals.
	 * @param inputStr
	 * @param decompString
	 * @return
	 */
	private double mongeElkanSimilarity(String inputStr, String decompString) {
		AbstractStringMetric metric = new MongeElkan();
		double sim = metric.getSimilarity(inputStr, decompString);
		return sim;
	}
	
	private double jaroWinklerSimilarity(String inputStr, String decompString) {
		AbstractStringMetric metric = new JaroWinkler();
		double sim = metric.getSimilarity(inputStr, decompString);
		return sim;
	}
	
	private double jaroSimilarity(String inputStr, String decompString) {
		AbstractStringMetric metric = new Jaro();
		double sim = metric.getSimilarity(inputStr, decompString);
		return sim;
	}
	
	private double levenstheinSimilarity(String inputStr, String decompString) {
		AbstractStringMetric metric = new Levenshtein();
		double sim = metric.getSimilarity(inputStr, decompString);
		return sim;
	}
	

	
	
	/**
	 * Compute geographic distance with the haversine formula.
	 * Earth radius expressed in Km.
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return distance in Km
	 */
	private double haversineDistance (double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 6371; // Km
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double sindLat = Math.sin(dLat / 2);
	    double sindLng = Math.sin(dLng / 2);
	    double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
	            * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    return dist;
  }
	
	
}


