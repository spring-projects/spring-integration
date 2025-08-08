/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.inbound;

import org.junit.Test;

import org.springframework.integration.jpa.core.JpaExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class JpaPollingChannelAdapterUnitTests {

	@Test
	public void testReceiveNull() {
		JpaExecutor jpaExecutor = mock(JpaExecutor.class);

		when(jpaExecutor.poll()).thenReturn(null);

		JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);
		assertThat(jpaPollingChannelAdapter.receive()).isNull();
	}

	@Test
	public void testReceiveNotNull() {
		JpaExecutor jpaExecutor = mock(JpaExecutor.class);

		when(jpaExecutor.poll()).thenReturn("Spring");

		JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);
		assertThat(jpaPollingChannelAdapter.receive()).as("Expecting a Message to be returned.").isNotNull();
		assertThat(jpaPollingChannelAdapter.receive().getPayload()).isEqualTo("Spring");
	}

	@Test
	public void testGetComponentType() {
		JpaExecutor jpaExecutor = mock(JpaExecutor.class);

		JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);
		assertThat(jpaPollingChannelAdapter.getComponentType()).isEqualTo("jpa:inbound-channel-adapter");
	}

}
