/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.jmx.inbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stuart Williams
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class MBeanAttributeFilterTests {

	@Autowired
	@Qualifier("out")
	private PollableChannel channel;

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Autowired
	private SourcePollingChannelAdapter adapterNot;

	@Autowired
	private String domain;

	private final long testTimeout = 10000L;

	@Test
	public void testAttributeFilter() {
		while (channel.receive(0) != null) {
			// drain
		}
		adapter.start();

		Message<?> result = channel.receive(testTimeout);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isInstanceOf(HashMap.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> payload = (Map<String, Object>) result.getPayload();
		assertThat(payload).hasSize(4);

		@SuppressWarnings("unchecked")
		Map<String, Object> beans = (Map<String, Object>) payload
				.get(this.domain + ":name=out,type=MessageChannel");

		assertThat(beans).containsOnlyKeys("QueueSize", "RemainingCapacity");

		adapter.stop();
	}

	@Test
	public void testAttributeFilterNot() {
		while (channel.receive(0) != null) {
			// drain
		}
		adapterNot.start();

		Message<?> result = channel.receive(testTimeout);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isInstanceOf(HashMap.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> payload = (Map<String, Object>) result.getPayload();
		assertThat(payload).hasSize(4);

		@SuppressWarnings("unchecked")
		Map<String, Object> bean = (Map<String, Object>) payload
				.get(domain + ":name=in,type=MessageChannel");

		List<String> keys = new ArrayList<>(bean.keySet());
		Collections.sort(keys);
		assertThat(keys).containsExactly("LoggingEnabled", "SubscriberCount");

		adapterNot.stop();
	}

}
