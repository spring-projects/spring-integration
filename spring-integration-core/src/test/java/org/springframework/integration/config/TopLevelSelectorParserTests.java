/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class TopLevelSelectorParserTests {

	@Autowired
	ApplicationContext context;

	@Test
	public void topLevelSelector() {
		MessageSelector selector = context.getBean("selector", MessageSelector.class);
		assertThat(selector.accept(new GenericMessage<>("test"))).isTrue();
	}

}
