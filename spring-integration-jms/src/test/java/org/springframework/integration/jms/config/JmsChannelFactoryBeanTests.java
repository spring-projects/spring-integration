/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Glenn Renfro
 */

class JmsChannelFactoryBeanTests {

	@Test
	void optionSetBeforeExternalTemplateAlsoThrows() {
		JmsChannelFactoryBean fb = new JmsChannelFactoryBean(false);
		fb.setBeanName("testChannel");
		fb.setDestinationName("testQueue");
		fb.setSessionTransacted(true);
		fb.setJmsTemplate(new JmsTemplate());
		fb.setBeanFactory(new StaticListableBeanFactory());

		assertThatIllegalArgumentException()
				.isThrownBy(fb::createInstance)
				.withMessageContaining("JmsTemplate properties must be configured on the externally supplied " +
						"JmsTemplate, not on the factory bean.");
	}

	@Test
	void externalTemplateAloneDoesNotThrowGuard() {
		JmsChannelFactoryBean fb = new JmsChannelFactoryBean(false);
		fb.setBeanName("testChannel");
		fb.setJmsTemplate(new JmsTemplate());
		fb.setDestinationName("testQueue");
		fb.setBeanFactory(new StaticListableBeanFactory());
		assertThat(fb.createInstance()).isNotNull();
	}

}
