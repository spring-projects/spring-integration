/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import java.io.IOException;

import javax.xml.transform.Result;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * @author Mark Fisher
 */
public class StubMarshaller implements Marshaller {

	@SuppressWarnings("rawtypes")
	public boolean supports(Class clazz) {
		return false;
	}

	public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
	}

}
