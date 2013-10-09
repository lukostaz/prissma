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

import com.hp.hpl.jena.rdf.model.Property;

public class Edge {

	public ContextUnit v1;
	public ContextUnit v2;
	public Property label;
	
	public String toString(){
		
		return label + ", from: " + v1 + "to:" + v2; 
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (!obj.getClass().equals(Edge.class))
				return false;
		
		Edge edge = (Edge) obj;
		
		if (this.v1.equals(edge.v1) && this.v2.equals(edge.v2) && this.label.equals(edge.label))
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {
		return  new String(v1.toString() + v2.toString() + label.toString()).hashCode();
	}
	
	
}
