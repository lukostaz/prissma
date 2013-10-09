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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;
import org.joda.time.format.PeriodFormatter;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import fr.inria.wimmics.prissma.selection.PrissmaProperties;
import fr.inria.wimmics.prissma.selection.exceptions.ContextUnitException;

public class ContextUnit{
	
	public CtxUnitType type;
	public RDFNode instance;

	
	public ContextUnit(CtxUnitType type) {
		this.type = type;
	}
	
	
	public String toString(){
		return instance + ", type: " + type;
	}


	/**
	 * Retrieves the specified property value from the context unit.
	 * Used by GEO and TIME context units.
	 * @param prop
	 * @return
	 * @throws ContextUnitException
	 */
	public double getComplexCtxUnitProp(Property prop) throws ContextUnitException {
		
		if (!instance.isResource())
			throw new ContextUnitException();
		
		if (this.type!= CtxUnitType.GEO && this.type!= CtxUnitType.TIME && instance.isResource())
			throw new ContextUnitException();
		else{
			Statement stat = ((Resource)instance).getProperty(prop);
			if (stat == null)
				throw new ContextUnitException();
			
			// TIME conversions
			if (prop.equals(PrissmaProperties.pStart)){
				String timeStr = stat.getLiteral().getString();
				DateTimeFormatter dtf = ISODateTimeFormat.timeParser();
				DateTime startTime = dtf.withZone(DateTimeZone.UTC).parseDateTime(timeStr); // UTC by default
				long millis = startTime.getMillis();
				long seconds = millis/1000;
				return seconds;
			} else if (prop.equals(PrissmaProperties.pDuration)){
				String durationStr = stat.getLiteral().getString();
				PeriodFormatter pf = ISOPeriodFormat.standard();
				Period period = pf.parsePeriod(durationStr);
				int seconds = period.toStandardSeconds().getSeconds();
				return seconds;
			}
			return stat.getLiteral().getDouble();
		}
	}


	@Override
	public boolean equals(Object obj) {
		
		if (!obj.getClass().equals(ContextUnit.class))
				return false;
		
		ContextUnit unitToComp = (ContextUnit) obj;
		
		if (this.type == unitToComp.type){
			
			if (this.type.equals(CtxUnitType.GEO) || this.type.equals(CtxUnitType.TIME)){
				if (this.instance.getModel().containsAll(unitToComp.instance.getModel()))
					return true;
				else 
					return false;
			}
			
			if (this.instance.equals(unitToComp.instance))
				return true;
			else
				return false;
		}
			
		else
			return false;
	}
	
	
	

//	@Override
//	public boolean equals(Object obj) {
//		
//		if (!obj.getClass().equals(ContextUnit.class))
//				return false;
//		
//		ContextUnit unitToComp = (ContextUnit) obj;
//		
//		if (this.type == unitToComp.type){
//			
//			if (this.type.equals(CtxUnitType.GEO) || this.type.equals(CtxUnitType.TIME)){
//				// does not care about URI of geo/time ctxunit root.
//				Model unitToCompModel = unitToComp.instance.getModel();
//				StmtIterator it = this.instance.getModel().listStatements();
//				while (it.hasNext()){
//					Statement stat = it.next();
//					RDFNode object = stat.getObject();
//					Property pred = stat.getPredicate();
//					if (!unitToCompModel.contains((Resource)null, pred, object))
//						return false;
//				}
//				return true;
//				
//			}
//			
//			
//			if (this.instance.equals(unitToComp.instance))
//				return true;
//			else
//				return false;
//		}
//			
//		else
//			return false;
//	}
	
	
	
	
	
	
	
	public int hashCode() {
		return  new String(type.toString() + instance.toString()).hashCode();
	}
	
}
