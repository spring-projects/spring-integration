/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.config.jsr223;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 */
@SpringJUnitConfig
public class Jsr223HeaderEnricherTests {

	@Autowired
	private MessageChannel inputA;

	@Autowired
	private QueueChannel outputA;

	@Autowired
	private MessageChannel inputB;

	@Autowired
	private QueueChannel outputB;

	@Test
	public void referencedScript() {
		inputA.send(new GenericMessage<>("Hello"));
		assertThat(outputA.receive(20000).getHeaders().get("TEST_HEADER")).isEqualTo("jruby");
	}

	@Test
	public void inlineScript() {
		inputB.send(new GenericMessage<>("Hello"));
		assertThat(outputB.receive(20000).getHeaders().get("TEST_HEADER")).isEqualTo("js");
	}

}
