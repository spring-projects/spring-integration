/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
