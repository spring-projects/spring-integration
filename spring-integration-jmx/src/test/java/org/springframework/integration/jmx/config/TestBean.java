/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
@ManagedResource
public class TestBean {

	final List<String> messages = new ArrayList<String>();

	@ManagedAttribute
	public String getFirstMessage() {
		return (messages.size() > 0) ? messages.get(0) : null;
	}

	@ManagedOperation
	public void test(String text) {
		this.messages.add(text);
	}

	@ManagedOperation
	public List<String> testWithReturn(String text) {
		this.messages.add(text);
		return messages;
	}

	@ManagedOperation
	public void testPrimitiveArgs(boolean bool, long time, int foo) {
		this.messages.add(bool + " " + time + " " + foo);
	}

}
