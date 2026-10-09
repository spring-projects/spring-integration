/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class EnricherParser2Tests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void configurationCheckRequiresReply() {
		Object endpoint = context.getBean("enricher");

		boolean requiresReply = TestUtils.<Boolean>getPropertyValue(endpoint, "handler.requiresReply");

		assertThat(requiresReply).as("Was expecting requiresReply to be 'false'").isFalse();
	}

}
