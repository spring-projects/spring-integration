/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class NestedChainParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void chainsAreNotSame() {
		ParentTestBean bean1 = context.getBean("concreteParent1", ParentTestBean.class);
		ParentTestBean bean2 = context.getBean("concreteParent2", ParentTestBean.class);
		assertThat(bean2).isNotSameAs(bean1);
		assertThat(bean2.chain).isNotSameAs(bean1.chain);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handlersAreNotSame() {
		ParentTestBean bean1 = context.getBean("concreteParent1", ParentTestBean.class);
		ParentTestBean bean2 = context.getBean("concreteParent2", ParentTestBean.class);
		List<MessageHandler> handlerList1 =
				(List<MessageHandler>) new DirectFieldAccessor(bean1.chain).getPropertyValue("handlers");
		List<MessageHandler> handlerList2 =
				(List<MessageHandler>) new DirectFieldAccessor(bean2.chain).getPropertyValue("handlers");
		MessageHandler handler1 = handlerList1.get(0);
		MessageHandler handler2 = handlerList2.get(0);
		assertThat(handler2).isNotSameAs(handler1);
	}

	static class ParentTestBean {

		private MessageHandlerChain chain;

		public void setChain(MessageHandlerChain chain) {
			this.chain = chain;
		}

	}

	static class ChildTestBean {

		public String echo(String s) {
			return s;
		}

	}

}
