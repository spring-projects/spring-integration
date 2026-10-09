/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class MethodInvokingSelectorParserTests {

	@Autowired
	private MessageSelectorChain chain;

	@SuppressWarnings("unchecked")
	@Test
	public void configOK() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(chain);
		List<MessageSelector> selectors = (List<MessageSelector>) accessor.getPropertyValue("selectors");
		assertThat(selectors.get(0)).isInstanceOf(MethodInvokingSelector.class);
	}

	public static class TestFilter {

		public boolean accept(Message<?> m) {
			return true;
		}

	}

}
