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

public class ETSubgraphIsomorphism implements Comparable<ETSubgraphIsomorphism>{

	public List<EditOperation> deltaList;
	public double cost;
	
		
	public ETSubgraphIsomorphism(){
		this.deltaList = new ArrayList<EditOperation>();
	}
	
	@Override
	public int compareTo(ETSubgraphIsomorphism o) {

		if (this.cost > o.cost)
			return 1;
		else if (this.cost == o.cost)
			return 0;
		else
			return -1;
	}
	
	public String toString(){
		return "cost: " + cost + 
				". Deltalist: " + deltaList.toString() + "\n";
	}
	
}
