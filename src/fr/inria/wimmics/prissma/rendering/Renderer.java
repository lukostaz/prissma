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

package fr.inria.wimmics.prissma.rendering;


import java.io.File;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.hp.hpl.jena.rdf.model.Model;

import fr.inria.jfresnel.FresnelDocument;
import fr.inria.jfresnel.RendererUtils;
import fr.inria.jfresnel.fsl.FSLHierarchyStore;
import fr.inria.jfresnel.fsl.FSLNSResolver;
import fr.inria.jfresnel.fsl.jena.FSLJenaEvaluator;
import fr.inria.jfresnel.fsl.jena.FSLJenaHierarchyStore;
import fr.inria.jfresnel.jena.FresnelJenaParser;
import fr.inria.jfresnel.jena.JenaRenderer;
import fr.inria.jfresnel.sparql.SPARQLNSResolver;
import fr.inria.jfresnel.sparql.jena.SPARQLJenaEvaluator;
import fr.inria.wimmics.prissma.selection.PrissmaProperties;



public class Renderer {
	
	private FSLNSResolver fnsr;
	private SPARQLNSResolver snsr;
	private FSLHierarchyStore fhs;
	private FresnelJenaParser fp;
	private FSLJenaEvaluator fje;
	private SPARQLJenaEvaluator sje;
	private  Logger LOG = LoggerFactory.getLogger(Renderer.class);

	
	
	public Renderer(){
		fnsr = new FSLNSResolver(); 
		snsr = new SPARQLNSResolver();
		fhs = new FSLJenaHierarchyStore();
		fp = new FresnelJenaParser(fnsr, fhs);
		fje = new FSLJenaEvaluator(fnsr, fhs); 
		sje = new SPARQLJenaEvaluator(snsr);
	}
	
	public String renderHTML(Model prism, Model inputModel, boolean isTest){
		
		FresnelDocument fd = fp.parse(prism, "");
		fje.setModel(inputModel);
		sje.setModel(inputModel);
		Document doc;
		try {
			JenaRenderer renderer = new JenaRenderer(); 
			doc = renderer.render(fd, fje, sje);
		} catch (javax.xml.parsers.ParserConfigurationException e) {
			LOG.error("Error while rendering RDF resource with Fresnel. ");
			return "";
		}
		
		String dir;
		if (isTest)
			dir = PrissmaProperties.ENTITIES_PATH_TEST;
		else
			dir = PrissmaProperties.ENTITIES_PATH;
		File xsltFile = new File(dir + PrissmaProperties.FRESNEL_XSLT_HTML);
        Source xsltSource = new StreamSource(xsltFile);
        String html = "";
		try {
			html = RendererUtils.transformDoc(doc, xsltSource);
		} catch (Exception e) {
			LOG.error("Error rendering RDF entity");
		}
		
		return html;
	}
	
	
	
}
