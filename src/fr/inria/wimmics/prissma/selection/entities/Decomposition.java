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

package fr.inria.wimmics.prissma.selection.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import fr.inria.wimmics.prissma.selection.utilities.ContextUnitConverter;

public class Decomposition {

	public List<DecompItem> elements; 
	public int idCounter;
	public static final int NO_FILTER = -1;
	
	// put here to check if a ctxunit is of type CLASS
	public Map<String, String> substitutions;
	
	
	public Decomposition(){
		this.elements = new ArrayList<DecompItem>();
		idCounter = 0;
	}


	@Override
	public boolean equals(Object dec) {
		if (!dec.getClass().equals(Decomposition.class))
			return false;
		Decomposition decomp = (Decomposition) dec;
		if (elements == null && decomp.elements == null)
			return true;
		if (elements.equals(decomp.elements))
			return true;
		else
			return false;
	}


	public int getItem(RDFNode sMax) {
		for (int i = 0; i < elements.size(); i++) {
			RDFNode mItem = getReconstructedModel(elements.get(i));
			if (ContextUnitConverter.areIsomorphic(mItem, sMax)){
				return i;
			}
		}
		return -2;
 	}
	
	

	public RDFNode getReconstructedModel(DecompItem item){
		RDFNode root;
		if(item.isCtxUnit){
			root = item.ctxUnit.instance;
		}else {
			root = reconstructRDFNodeForItem(item);
		}
		return root;
	}
	
	private RDFNode reconstructRDFNodeForItem(DecompItem item){
		
		if (item.isCtxUnit){
			return item.ctxUnit.instance;
		}
			

		// get connecting edge
		// (even if there should be one edge only, due to prissma declarations nature)
		Model nodeModel = ModelFactory.createDefaultModel();
		
		for (Edge edge : item.edges) {
			Property pred = edge.label;
			Resource s = edge.v1.instance.asResource();
			RDFNode o = edge.v2.instance;
			nodeModel.add(s, pred, o);
		}
		
		// get stuff from ancestors
		RDFNode m1 = reconstructRDFNodeForItem(this.elements.get(item.idAncestor1));
		RDFNode m2 = reconstructRDFNodeForItem(this.elements.get(item.idAncestor2));
		
		Model m1Model = m1.getModel();
		Model m2Model = m2.getModel();
		// if no model present, m1 and m2 are (simple) ctxUnits.
		// In this case, there is no need to add them, as they have already been added as edge nodes before.
		if (m1Model != null && !m1Model.isEmpty() )
			nodeModel.add(m1Model);
		if ( m2Model != null && !m2Model.isEmpty()  )
			nodeModel.add(m2Model);
		
		return ContextUnitConverter.getRoot(nodeModel, true);
	}


	@Override
	public String toString() {
		return this.elements.toString();
	}
	

	
	public int getSize(){
		if (this.idCounter == 0){
			return 0;
		} else
			return this.idCounter -1;
	}


	public int getPrismCount() {
		int count = 0;
		for (DecompItem item : this.elements) {
			count += item.prismURISet.size();
		}
		return count;
	}
	
	
	
}
