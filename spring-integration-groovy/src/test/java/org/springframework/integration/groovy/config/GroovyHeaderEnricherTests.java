/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class GroovyHeaderEnricherTests {

	@Autowired
	private MessageChannel inputA;

	@Autowired
	private QueueChannel outputA;

	@Autowired
	private MessageChannel inputB;

	@Autowired
	private QueueChannel outputB;

	@Autowired
	private EventDrivenConsumer headerEnricherWithInlineGroovyScript;

	@Test
	public void referencedScript() {
		inputA.send(new GenericMessage<>("Hello"));
		assertThat(outputA.receive(1000).getHeaders().get("TEST_HEADER")).isEqualTo("groovy");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void inlineScript() {
		Map<String, HeaderValueMessageProcessor<?>> headers =
				TestUtils.getPropertyValue(headerEnricherWithInlineGroovyScript,
						"handler.transformer.headersToAdd", Map.class);
		assertThat(headers.size()).isEqualTo(1);
		HeaderValueMessageProcessor<?> headerValueMessageProcessor = headers.get("TEST_HEADER");
		assertThat(headerValueMessageProcessor.getClass().getName())
				.contains("MessageProcessingHeaderValueMessageProcessor");
		Object targetProcessor = TestUtils.getPropertyValue(headerValueMessageProcessor, "targetProcessor");
		assertThat(targetProcessor.getClass()).isEqualTo(GroovyScriptExecutingMessageProcessor.class);

		inputB.send(new GenericMessage<String>("Hello"));
		assertThat(outputB.receive(1000).getHeaders().get("TEST_HEADER")).isEqualTo("groovy");
	}

}
