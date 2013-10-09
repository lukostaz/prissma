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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

import fr.inria.wimmics.prissma.selection.utilities.ContextUnitConverter;


public class DecompItem {

	public int id;
	public int idAncestor1 = -1;
	public int idAncestor2 = -1;
	public List<Edge> edges;
	
	// only used if the item is a prism
	public Set<URI> prismURISet;
	
	// only set for ctx units
	public ContextUnit ctxUnit;
	public boolean isCtxUnit; //FIXME remove: useless. (ctxUnit == null | ctxunit != null)


	public DecompItem(int id) {
		super();
		this.id = id;
		edges = new ArrayList<Edge>();
		isCtxUnit = false;
		this.prismURISet = new HashSet<URI>();
	}


	public DecompItem(
			Decomposition decomp,
			RDFNode g,
			RDFNode sMax, 
			RDFNode gMinusSmax, 
			Edge e, 
			URI prismURI) {
		
		this.edges = new ArrayList<Edge>();
		this.id = decomp.idCounter;
		this.prismURISet = new HashSet<URI>();
		if (prismURI != null) 
			this.prismURISet.add(prismURI);
		
		// create non-ctx unit decomp item
		if (sMax != null && gMinusSmax != null && e != null){
			this.isCtxUnit = false;
			this.idAncestor1 = decomp.getItem(sMax);
			this.idAncestor2 = decomp.getItem(gMinusSmax);
			this.edges.add(e);
		}else{
			// create new g decomp element (context unit)
			this.isCtxUnit = true;
			ContextUnitConverter ctxUnitConverter = new ContextUnitConverter();
			ctxUnitConverter.convertInputToUnits(g, decomp.substitutions.values());
			ContextUnit ctxunit = null;
			// there is only one item in the set because g is ctxunit
			Iterator<ContextUnit> it = ctxUnitConverter.inputGraphContextUnits.iterator();
			while (it.hasNext())
				ctxunit = it.next();
			this.ctxUnit = ctxunit;
		}
	}



	public boolean isPrism(){
		if (prismURISet.isEmpty())
			return false;
		else
			return true;
	}

	
	
	
	@Override
	public String toString() {
		
		StringBuffer strbuf = new StringBuffer();
		if (!prismURISet.isEmpty()){
			strbuf.append("\nPRISM: ");
			for (URI prismURI : prismURISet) 
				strbuf.append(prismURI + ", ");
			strbuf.append("\n");
		}
		
		if (isCtxUnit)
			strbuf.append( "id: " + id + ". " +
					"CtxUnit: " + ctxUnit + "\n ");
		else
			strbuf.append( "id: " + id + ". \n" +
					"Ancestor1: " + idAncestor1 + "\n" +
					"Ancestor2: " + idAncestor2 + "\n" +
					"Edges: " + edges.toString() + "\n");
		return strbuf.toString();
	}



	@Override
	public boolean equals(Object obj) {
		if (!obj.getClass().equals(DecompItem.class))
			return false;
		DecompItem item = (DecompItem) obj;
		
		if (//id == item.id && 
			idAncestor1 == item.idAncestor1 &&
			idAncestor2 == item.idAncestor2 &&
			edges.equals(item.edges) ){
			
			if (ctxUnit != null){
				if (ctxUnit.equals(item.ctxUnit))
					return true;
				else 
					return false;
			}else{
				if (item.ctxUnit == null)
					return true;
				else
					return false;
			}
		} else
			return false;
	}

	


	
	
}
