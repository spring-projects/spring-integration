/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.http;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author Artem Bilan
 *
 * @since 3.0
 */
public abstract class AbstractHttpInboundTests {

	@BeforeEach
	public void setupHttpInbound() {
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
	}

	@AfterEach
	public void tearDownHttpInbound() {
		RequestContextHolder.resetRequestAttributes();
	}

}
