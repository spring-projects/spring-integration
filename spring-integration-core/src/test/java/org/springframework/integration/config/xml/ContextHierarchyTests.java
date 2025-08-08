/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
public class ContextHierarchyTests {

	private ApplicationContext parentContext;

	private ApplicationContext childContext;

	@Before
	public void setupContext() {
		String prefix = "/org/springframework/integration/config/xml/ContextHierarchyTests-";
		this.parentContext = new ClassPathXmlApplicationContext(prefix + "parent.xml");
		this.childContext = new ClassPathXmlApplicationContext(
				new String[] {prefix + "child.xml"}, parentContext);
	}

	@Test // INT-646
	public void inputChannelInParentContext() {
		Object parentInput = parentContext.getBean("input");
		Object childInput = childContext.getBean("input");
		Object endpoint = childContext.getBean("chain");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		Object endpointInput = accessor.getPropertyValue("inputChannel");
		assertThat(childInput).isEqualTo(parentInput);
		assertThat(endpointInput).isEqualTo(parentInput);
	}

}
