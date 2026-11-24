/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.springframework.ws.client.core.SourceExtractor;

/**
 * @author Mark Fisher
 */
public class StubSourceExtractor implements SourceExtractor<Object> {

	public Object extractData(Source source) throws IOException, TransformerException {
		return "test";
	}

}
