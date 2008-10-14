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

import java.util.List;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.integration.selector.MessageSelectorChain;

/**
 * @author Mark Fisher
 */
public class SelectorChainParserTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testSelectorChain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"selectorChainParserTests.xml", this.getClass());
		MessageSelector selector1 = (MessageSelector) context.getBean("selector1");
		MessageSelector selector2 = (MessageSelector) context.getBean("selector2");
		MessageSelectorChain chain = (MessageSelectorChain) context.getBean("selectorChain");
		DirectFieldAccessor accessor = new DirectFieldAccessor(chain);
		List<MessageSelector> selectors = (List<MessageSelector>)
				accessor.getPropertyValue("selectors");
		assertEquals(selector1, selectors.get(0));
		assertEquals(selector2, selectors.get(1));
	}

}
