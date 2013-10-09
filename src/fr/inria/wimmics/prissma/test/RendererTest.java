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

package fr.inria.wimmics.prissma.test;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import fr.inria.wimmics.prissma.rendering.Renderer;
import fr.inria.wimmics.prissma.selection.PrissmaProperties;

public class RendererTest {

	@Test
	public void testSimpleRender() {

		Model fresnelDecl = ModelFactory.createDefaultModel();
		InputStream in = FileManager.get().open( PrissmaProperties.INACTIVE_PRISMS_TEST + 
				"prism2.ttl" );
		if (in != null) {
			fresnelDecl.read(in, null,  "TURTLE");
		}
		
		Model inputResource = ModelFactory.createDefaultModel();
		in = FileManager.get().open( PrissmaProperties.ENTITIES_PATH_TEST + 
				"foaf.ttl" );
		if (in != null) {
			inputResource.read(in, null,  "TURTLE");
		}
		
		Renderer r = new Renderer();
		String html = r.renderHTML(fresnelDecl, inputResource, true);
		assertNotNull(html);
		
	}

}
