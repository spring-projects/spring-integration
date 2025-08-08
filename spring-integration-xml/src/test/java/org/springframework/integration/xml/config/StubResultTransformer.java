/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import javax.xml.transform.Result;

import org.springframework.integration.xml.transformer.ResultTransformer;

public class StubResultTransformer implements ResultTransformer {

	Object toReturn;

	public StubResultTransformer(Object toReturn) {
		this.toReturn = toReturn;
	}

	public Object transformResult(Result res) {
		return toReturn;
	}

}
