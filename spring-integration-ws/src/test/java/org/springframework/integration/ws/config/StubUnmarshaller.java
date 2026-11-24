/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import java.io.IOException;

import javax.xml.transform.Source;

import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * @author Mark Fisher
 */
public class StubUnmarshaller implements Unmarshaller {

	@SuppressWarnings("rawtypes")
	public boolean supports(Class clazz) {
		return false;
	}

	public Object unmarshal(Source source) throws XmlMappingException, IOException {
		return null;
	}

}
