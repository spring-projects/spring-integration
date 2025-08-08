/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import java.util.LinkedList;

import javax.xml.transform.Source;

import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 *
 * @author Jonas Partner
 * @author Artem Bilan
 *
 */
public class StubUnmarshaller implements Unmarshaller {

	public final LinkedList<Source> sourcesPassed = new LinkedList<>();

	public boolean supports(Class<?> clazz) {
		return true;
	}

	public Object unmarshal(Source source) throws XmlMappingException {
		this.sourcesPassed.addFirst(source);
		return "unmarshalled";
	}

}
