/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
