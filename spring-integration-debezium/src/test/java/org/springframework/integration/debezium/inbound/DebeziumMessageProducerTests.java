/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.debezium.inbound;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class DebeziumMessageProducerTests {

	DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumBuilderMock;

	DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngineMock;

	DebeziumMessageProducer debeziumMessageProducer;

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void beforeEach() {
		debeziumBuilderMock = mock(DebeziumEngine.Builder.class);
		debeziumEngineMock = mock(DebeziumEngine.class);
		given(debeziumBuilderMock.notifying(any(Consumer.class))).willReturn(debeziumBuilderMock);
		given(debeziumBuilderMock.build()).willReturn(debeziumEngineMock);

		debeziumMessageProducer = new DebeziumMessageProducer(debeziumBuilderMock);
		debeziumMessageProducer.setOutputChannel(new QueueChannel());
		debeziumMessageProducer.setBeanFactory(mock(BeanFactory.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDebeziumMessageProducerLifecycle() throws IOException {

		debeziumMessageProducer.afterPropertiesSet(); // INIT

		then(debeziumBuilderMock).should().build();
		assertThat(debeziumMessageProducer.isActive()).isEqualTo(false);
		assertThat(debeziumMessageProducer.isRunning()).isEqualTo(false);

		debeziumMessageProducer.start(); // START

		await().atMost(5, TimeUnit.SECONDS).until(() -> debeziumMessageProducer.isRunning());
		assertThat(debeziumMessageProducer.isActive()).isEqualTo(true);

		debeziumMessageProducer.stop(); // STOP

		assertThat(debeziumMessageProducer.isActive()).isEqualTo(false);
		assertThat(debeziumMessageProducer.isRunning()).isEqualTo(false);

		// The DebeziumEngine is started on a different thread.
		// Only the way to catch the run() mock is to stop DebeziumMessageProducer and wait for its internal latch
		then(debeziumEngineMock).should().run();
		then(debeziumEngineMock).should().close();

		reset(debeziumEngineMock);

		debeziumMessageProducer.start(); // START

		await().atMost(5, TimeUnit.SECONDS).until(() -> debeziumMessageProducer.isRunning());
		assertThat(debeziumMessageProducer.isActive()).isEqualTo(true);

		debeziumMessageProducer.destroy(); // DESTROY

		then(debeziumEngineMock).should().run();
		then(debeziumEngineMock).should().close();
	}

}
