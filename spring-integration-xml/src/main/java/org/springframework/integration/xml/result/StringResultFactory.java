/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.result;

import javax.xml.transform.Result;

import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 */
public class StringResultFactory implements ResultFactory {

	public Result createResult(Object payload) {
		return new StringResult();
	}

}
