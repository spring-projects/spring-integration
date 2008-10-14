/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.integration.selector.MessageSelectorChain.Strategy;

/**
 * @author Mark Fisher
 */
public class SelectorChainParserTests {

	@Test
	public void selectorChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"selectorChainParserTests.xml", this.getClass());
		MessageSelector selector1 = (MessageSelector) context.getBean("selector1");
		MessageSelector selector2 = (MessageSelector) context.getBean("selector2");
		MessageSelectorChain chain = (MessageSelectorChain) context.getBean("selectorChain");
		List<MessageSelector> selectors = this.getSelectors(chain);
		assertEquals(Strategy.ALL, this.getStrategy(chain));
		assertEquals(selector1, selectors.get(0));
		assertEquals(selector2, selectors.get(1));
		assertTrue(chain.accept(new StringMessage("test")));
	}

	@Test
	public void nestedSelectorChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"selectorChainParserTests.xml", this.getClass());
		MessageSelector selector1 = (MessageSelector) context.getBean("selector1");
		MessageSelector selector2 = (MessageSelector) context.getBean("selector2");
		MessageSelector selector3 = (MessageSelector) context.getBean("selector3");
		MessageSelector selector4 = (MessageSelector) context.getBean("selector4");
		MessageSelector selector5 = (MessageSelector) context.getBean("selector5");
		MessageSelector selector6 = (MessageSelector) context.getBean("selector6");
		MessageSelectorChain chain1 = (MessageSelectorChain) context.getBean("nestedSelectorChain");
		assertEquals(Strategy.MORE_THAN_HALF, this.getStrategy(chain1));
		List<MessageSelector> selectorList1 = this.getSelectors(chain1);
		assertEquals(selector1, selectorList1.get(0));
		assertTrue(selectorList1.get(1) instanceof MessageSelectorChain);
		MessageSelectorChain chain2 = (MessageSelectorChain) selectorList1.get(1);
		assertEquals(Strategy.ALL, this.getStrategy(chain2));
		List<MessageSelector> selectorList2 = this.getSelectors(chain2);
		assertEquals(selector2, selectorList2.get(0));
		assertTrue(selectorList2.get(1) instanceof MessageSelectorChain);
		MessageSelectorChain chain3 = (MessageSelectorChain) selectorList2.get(1);
		assertEquals(Strategy.ANY, this.getStrategy(chain3));
		List<MessageSelector> selectorList3 = this.getSelectors(chain3);
		assertEquals(selector3, selectorList3.get(0));
		assertEquals(selector4, selectorList3.get(1));
		assertEquals(selector5, selectorList2.get(2));
		assertTrue(selectorList1.get(2) instanceof MessageSelectorChain);
		MessageSelectorChain chain4 = (MessageSelectorChain) selectorList1.get(2);
		assertEquals(Strategy.AT_LEAST_HALF, this.getStrategy(chain4));
		List<MessageSelector> selectorList4 = this.getSelectors(chain4);
		assertEquals(selector6, selectorList4.get(0));
		assertTrue(chain1.accept(new StringMessage("test1")));
		assertTrue(chain2.accept(new StringMessage("test2")));
		assertTrue(chain3.accept(new StringMessage("test3")));
		assertTrue(chain4.accept(new StringMessage("test4")));
	}


	@SuppressWarnings("unchecked")
	private List<MessageSelector> getSelectors(MessageSelectorChain chain) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(chain);
		return (List<MessageSelector>) accessor.getPropertyValue("selectors");
	}

	private Strategy getStrategy(MessageSelectorChain chain) {
		return (Strategy) new DirectFieldAccessor(chain).getPropertyValue("strategy");
	}

}
