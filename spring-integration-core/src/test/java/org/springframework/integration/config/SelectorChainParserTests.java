/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.integration.selector.MessageSelectorChain.VotingStrategy;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class SelectorChainParserTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void selectorChain() {
		MessageSelector selector1 = (MessageSelector) context.getBean("selector1");
		MessageSelector selector2 = (MessageSelector) context.getBean("selector2");
		MessageSelectorChain chain = (MessageSelectorChain) context.getBean("selectorChain");
		List<MessageSelector> selectors = this.getSelectors(chain);
		assertThat(this.getStrategy(chain)).isEqualTo(VotingStrategy.ALL);
		assertThat(selectors.get(0)).isEqualTo(selector1);
		assertThat(selectors.get(1)).isEqualTo(selector2);
		assertThat(chain.accept(new GenericMessage<>("test"))).isTrue();
		assertThat(this.context.containsBean("pojoSelector")).isTrue();
	}

	@Test
	public void nestedSelectorChain() {
		MessageSelector selector1 = (MessageSelector) context.getBean("selector1");
		MessageSelector selector2 = (MessageSelector) context.getBean("selector2");
		MessageSelector selector3 = (MessageSelector) context.getBean("selector3");
		MessageSelector selector4 = (MessageSelector) context.getBean("selector4");
		MessageSelector selector5 = (MessageSelector) context.getBean("selector5");
		MessageSelector selector6 = (MessageSelector) context.getBean("selector6");
		MessageSelectorChain chain1 = (MessageSelectorChain) context.getBean("nestedSelectorChain");
		assertThat(this.getStrategy(chain1)).isEqualTo(VotingStrategy.MAJORITY);
		List<MessageSelector> selectorList1 = this.getSelectors(chain1);
		assertThat(selectorList1.get(0)).isEqualTo(selector1);
		assertThat(selectorList1.get(1) instanceof MessageSelectorChain).isTrue();
		MessageSelectorChain chain2 = (MessageSelectorChain) selectorList1.get(1);
		assertThat(this.getStrategy(chain2)).isEqualTo(VotingStrategy.ALL);
		List<MessageSelector> selectorList2 = this.getSelectors(chain2);
		assertThat(selectorList2.get(0)).isEqualTo(selector2);
		assertThat(selectorList2.get(1) instanceof MessageSelectorChain).isTrue();
		MessageSelectorChain chain3 = (MessageSelectorChain) selectorList2.get(1);
		assertThat(this.getStrategy(chain3)).isEqualTo(VotingStrategy.ANY);
		List<MessageSelector> selectorList3 = this.getSelectors(chain3);
		assertThat(selectorList3.get(0)).isEqualTo(selector3);
		assertThat(selectorList3.get(1)).isEqualTo(selector4);
		assertThat(selectorList2.get(2)).isEqualTo(selector5);
		assertThat(selectorList1.get(2) instanceof MessageSelectorChain).isTrue();
		MessageSelectorChain chain4 = (MessageSelectorChain) selectorList1.get(2);
		assertThat(this.getStrategy(chain4)).isEqualTo(VotingStrategy.MAJORITY_OR_TIE);
		List<MessageSelector> selectorList4 = this.getSelectors(chain4);
		assertThat(selectorList4.get(0)).isEqualTo(selector6);
		assertThat(chain1.accept(new GenericMessage<>("test1"))).isTrue();
		assertThat(chain2.accept(new GenericMessage<>("test2"))).isTrue();
		assertThat(chain3.accept(new GenericMessage<>("test3"))).isTrue();
		assertThat(chain4.accept(new GenericMessage<>("test4"))).isTrue();
	}

	private List<MessageSelector> getSelectors(MessageSelectorChain chain) {
		return TestUtils.getPropertyValue(chain, "selectors");
	}

	private VotingStrategy getStrategy(MessageSelectorChain chain) {
		return TestUtils.getPropertyValue(chain, "votingStrategy");
	}

	public static class StubPojoSelector {

		public boolean accept(Message<?> message) {
			return true;
		}

	}

}
