/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.context.IntegrationContextUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class DefaultConfiguringBeanFactoryPostProcessorHierarchyTests {

	@Test
	public void verifySinglePostProcessor() {
		ClassPathXmlApplicationContext parent = new ClassPathXmlApplicationContext(
				"hierarchyTests-parent.xml", this.getClass());
		parent.refresh();
		GenericApplicationContext child = new GenericApplicationContext();
		child.setParent(parent);
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(child);
		reader.loadBeanDefinitions(new ClassPathResource("hierarchyTests-child.xml", this.getClass()));
		child.refresh();
		assertThat(child.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME))
				.isSameAs(parent.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME));
		assertThat(child.getBean(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME))
				.isSameAs(parent.getBean(IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME));
		assertThat(child.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME))
				.isSameAs(parent.getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME));

		child.close();
		parent.close();
	}

}
