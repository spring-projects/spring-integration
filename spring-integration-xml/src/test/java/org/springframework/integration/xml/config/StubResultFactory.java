/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import javax.xml.transform.Result;

import org.springframework.integration.xml.result.ResultFactory;
import org.springframework.xml.transform.StringResult;

public class StubResultFactory implements ResultFactory {

	public Result createResult(Object payload) {
		return new StubStringResult();
	}

	public static class StubStringResult extends StringResult {

	}

}
