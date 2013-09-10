/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.messaging.Message;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.FilterFactoryBean;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class FilterAnnotationMethodResolutionTests {

	@Test
	public void resolveAnnotatedMethod() throws Exception {
		FilterFactoryBean factoryBean = new FilterFactoryBean();
		factoryBean.setBeanFactory(new DefaultListableBeanFactory());
		AnnotatedTestFilter filter = new AnnotatedTestFilter();
		factoryBean.setTargetObject(filter);
		MessageHandler handler = factoryBean.getObject();
		QueueChannel replyChannel = new QueueChannel();
		handler.handleMessage(MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build());
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertTrue(filter.invokedCorrectMethod);
		assertFalse(filter.invokedIncorrectMethod);
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
