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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

import fr.inria.wimmics.prissma.selection.entities.StringSimilarity;

public class PrissmaProperties {
	
	
	// edit operation costs
	public static final double MAX = 1;
	public static final double MIN = 0;
	public static final double MISSING_CTXUNIT_ENTITY_COST = .4;
	public static final double MISSING_CTXUNIT_STRING_COST = MISSING_CTXUNIT_ENTITY_COST;
	
	// decomposition constants
	public static final int CTXUNIT_SUBJ = 1;
	public static final int CTXUNIT_OBJ = 2;
	public static final int NO_CTXUNIT = 0;
	public static final int NO_FILTER = -1;
	
	// matcher parameters
	// THRESHOLD = 0: perfect match needed to find prism
	public static double THRESHOLD = .7;
	public static final double DECAY_CONSTANT_TIME = 10;
	public static final double DECAY_CONSTANT_GEO = 5;
	public static final StringSimilarity STRING_SIMILARITY = StringSimilarity.MONGE_ELKAN;
	
	// Properties constants
	public static final String DEFAULT = "http://example.org#";
	public static final String GEO = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	public static final String PRISSMA = "http://ns.inria.fr/prissma/v2#";
	public static final String TL = "http://purl.org/NET/c4dm/timeline.owl#";
	public static final String AO = "http://purl.org/ontology/ao/core#";
	public static final String FOAF = "http://xmlns.com/foaf/0.1/";
	public static final String FRESNEL = "http://www.w3.org/2004/09/fresnel#";
	public static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String XSD = "http://www.w3.org/2001/XMLSchema#";
	
	private static Model m = ModelFactory.createDefaultModel();
	public static Property pLat = PrissmaProperties.m.createProperty(PrissmaProperties.GEO + "lat");
	public static Property pLon = PrissmaProperties.m.createProperty(PrissmaProperties.GEO + "lon");
	public static Property pRad = PrissmaProperties.m.createProperty(PrissmaProperties.PRISSMA + "radius");
	public static Property pStart = PrissmaProperties.m.createProperty(PrissmaProperties.TL + "start");
	public static Property pDuration = PrissmaProperties.m.createProperty(PrissmaProperties.TL + "duration");
	public static Property pTime = PrissmaProperties.m.createProperty(PrissmaProperties.AO + "time");
	public static Property pKnows = PrissmaProperties.m.createProperty(PrissmaProperties.FOAF + "knows");
	public static Property pShowProperties = PrissmaProperties.m.createProperty(PrissmaProperties.FRESNEL + "showProperties");
	public static Property pHideProperties = PrissmaProperties.m.createProperty(PrissmaProperties.FRESNEL + "hideProperties");
	public static Property pPurpose = PrissmaProperties.m.createProperty(PrissmaProperties.FRESNEL + "purpose");
	public static Property pPOI = PrissmaProperties.m.createProperty(PrissmaProperties.PRISSMA + "poi");
	public static Property pUsr = PrissmaProperties.m.createProperty(PrissmaProperties.PRISSMA + "user");
	public static Property pDev = PrissmaProperties.m.createProperty(PrissmaProperties.PRISSMA + "device");
	public static Property pEnv = PrissmaProperties.m.createProperty(PrissmaProperties.PRISSMA + "environment");
	public static Property pType = PrissmaProperties.m.createProperty(PrissmaProperties.RDF + "type");
	
	
	public static Property pSpurious1 = PrissmaProperties.m.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#first");
	public static Property pSpurious2 = PrissmaProperties.m.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");
	
	
	public static final Property priorityCutProperties[] = {pUsr, pEnv, pDev, pPOI, pTime};
	public static final Property internalCtxUnitProperties[] = {pLat, pLon, pRad, pStart, pDuration};
	public static final Property internalGEOProperties[] = {pLat, pLon, pRad};
	public static final Property internalTIMEProperties[] = {pStart, pDuration};
	

	// I/O
	public static final String PRISM_PATH_TEST = "test-campaign/test5/full/prisms/";
	public static final String ENTITIES_PATH_TEST = "entities/";
	public static final String ACTUAL_CTX_PATH_TEST = "test-campaign/test5/full/ctx/";
	public static final String INACTIVE_PRISMS_TEST = "inactive_prisms/";
	
	public static final String PRISM_PATH = "/sdcard/PRISSMA/prisms/";
	public static final String ENTITIES_PATH = "/sdcard/PRISSMA/entities/";
	public static final String ACTUAL_CTX_PATH = "/sdcard/PRISSMA/ctx/";
//	public static final String PRISM_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PRISSMA/prisms/";
//	public static final String ENTITIES_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() +  "/PRISSMA/entities/";
//	public static final String ACTUAL_CTX_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() +  "/PRISSMA/ctx/";
	
	
	
	public static final String PRISM1_FILENAME = "prism1.ttl";
	public static final String ACTUAL_CTX_FILENAME = "ctx.ttl";
	public static final String FRESNEL_1_FILENAME = "fresnel1.ttl";
	public static final String FOAF_PERSON_TEST = "foaf.ttl";
	public static final String FRESNEL_XSLT_HTML = "FDL2HTML.xsl";
	
	
	
	
	

	
	
}
