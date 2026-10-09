/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.ip.config;

import org.junit.jupiter.api.Test;

import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1.1
 */
public class TcpConnectionFactoryFactoryBeanTests implements TestApplicationContextAware {

	@Test
	public void testNoReadDelay() throws Exception {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean();
		fb.setHost("foo");
		fb.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		fb.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		fb.afterPropertiesSet();
		// INT-3578 IllegalArgumentException on 'readDelay'
		assertThat(TestUtils.getPropertyValue(fb.getObject(), "readDelay")).isEqualTo(100L);
	}

	@Test
	public void testReadDelay() throws Exception {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean();
		fb.setHost("foo");
		fb.setReadDelay(1000);
		fb.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		fb.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		fb.afterPropertiesSet();
		assertThat(TestUtils.getPropertyValue(fb.getObject(), "readDelay")).isEqualTo(1000L);
	}

}
