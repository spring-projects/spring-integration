/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ContextHierarchyTests {

	@Test
	public void inputChannelInParentContext() {
		String prefix = "/org/springframework/integration/config/xml/ContextHierarchyTests-";
		ConfigurableApplicationContext parentContext = new ClassPathXmlApplicationContext(prefix + "parent.xml");
		ConfigurableApplicationContext childContext = new ClassPathXmlApplicationContext(
				new String[] {prefix + "child.xml"}, parentContext);

		Object parentInput = parentContext.getBean("input");
		Object childInput = childContext.getBean("input");
		Object endpoint = childContext.getBean("chain");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		Object endpointInput = accessor.getPropertyValue("inputChannel");
		assertThat(childInput).isEqualTo(parentInput);
		assertThat(endpointInput).isEqualTo(parentInput);

		parentContext.close();
		childContext.close();
	}

}
