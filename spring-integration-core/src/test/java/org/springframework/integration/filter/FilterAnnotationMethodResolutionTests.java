/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.filter;

import org.junit.Test;

import org.springframework.integration.annotation.Filter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.FilterFactoryBean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artme Bilan
 *
 * @since 2.0
 */
public class FilterAnnotationMethodResolutionTests {

	@Test
	public void resolveAnnotatedMethod() throws Exception {
		TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();
		testApplicationContext.refresh();
		FilterFactoryBean factoryBean = new FilterFactoryBean();
		factoryBean.setBeanFactory(testApplicationContext);
		AnnotatedTestFilter filter = new AnnotatedTestFilter();
		factoryBean.setTargetObject(filter);
		MessageHandler handler = factoryBean.getObject();
		QueueChannel replyChannel = new QueueChannel();
		handler.handleMessage(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(filter.invokedCorrectMethod).isTrue();
		assertThat(filter.invokedIncorrectMethod).isFalse();
		testApplicationContext.close();
	}

	public static class AnnotatedTestFilter {

		private volatile boolean invokedCorrectMethod;

		private volatile boolean invokedIncorrectMethod;

		public String notThisOne(String s) {
			this.invokedIncorrectMethod = true;
			return s;
		}

		public void norThisOne(String s) {
			this.invokedIncorrectMethod = true;
		}

		public boolean andNotEvenThisOne(String s) {
			this.invokedIncorrectMethod = true;
			return true;
		}

		@Filter
		public boolean thisOne(String s) {
			this.invokedCorrectMethod = true;
			return true;
		}

	}

}
