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
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * @author Christian Tzolov
 */
public class DebeziumMessageProducerTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testEndpointLifecycle() throws IOException {
		DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumBuilderMock = mock(DebeziumEngine.Builder.class);
		DebeziumEngine<ChangeEvent<byte[], byte[]>> debeziumEngineMock = mock(DebeziumEngine.class);
		given(debeziumBuilderMock.notifying(any(Consumer.class))).willReturn(debeziumBuilderMock);
		given(debeziumBuilderMock.build()).willReturn(debeziumEngineMock);

		DebeziumMessageProducer debeziumMessageProducer = new DebeziumMessageProducer(debeziumBuilderMock);
		debeziumMessageProducer.setOutputChannel(new QueueChannel());
		debeziumMessageProducer.setBeanFactory(mock(BeanFactory.class));

		debeziumMessageProducer.afterPropertiesSet();
//		then(debeziumEngineMock).should(never()).run();

		debeziumMessageProducer.start();
		then(debeziumEngineMock).should().run();

		debeziumMessageProducer.stop();

		debeziumMessageProducer.destroy();
		then(debeziumEngineMock).should().close();
	}
}
