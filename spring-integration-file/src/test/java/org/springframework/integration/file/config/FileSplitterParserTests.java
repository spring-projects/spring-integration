/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.config;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FileSplitterParserTests {

	@Autowired
	private EventDrivenConsumer fullBoat;

	@Autowired
	private FileSplitter splitter;

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel out;

	@Test
	public void testComplete() {
		assertThat(TestUtils.getPropertyValue(this.splitter, "returnIterator", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.splitter, "markers", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.splitter, "markersJson", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.splitter, "requiresReply", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.splitter, "applySequence", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.splitter, "charset")).isEqualTo(StandardCharsets.UTF_8);
		assertThat(TestUtils.getPropertyValue(this.splitter, "messagingTemplate.sendTimeout")).isEqualTo(5L);
		assertThat(TestUtils.getPropertyValue(this.splitter, "firstLineHeaderName")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(this.splitter, "discardChannelName")).isEqualTo("nullChannel");
		assertThat(this.splitter.getOutputChannel()).isSameAs(this.out);
		assertThat(this.splitter.getOrder()).isEqualTo(2);
		assertThat(this.fullBoat.getInputChannel()).isSameAs(this.in);
		assertThat(this.fullBoat.isAutoStartup()).isFalse();
		assertThat(this.fullBoat.getPhase()).isEqualTo(1);
	}

}
