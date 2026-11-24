/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xml.config;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StringSource;

/**
 *
 * @author Jonas Partner
 * @author Artem Bilan
 *
 */
public class StubMarshaller implements Marshaller {

	public void marshal(Object graph, Result result) throws XmlMappingException {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			StringSource stringSource = new StringSource("""
					<?xml version="1.0" encoding="ISO-8859-1"?>
					<root>""" + graph + "</root>");
			transformer.transform(stringSource, result);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public boolean supports(Class<?> clazz) {
		return true;
	}

}
