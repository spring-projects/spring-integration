/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.debezium.it;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.KeyValueHeaderChangeEventFormat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.debezium.DebeziumMySqlTestContainer;
import org.springframework.messaging.MessageChannel;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
@Configuration
@EnableIntegration
public class DebeziumTestConfiguration {

	@Bean
	public DebeziumEngine.Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder() {

		return DebeziumEngine.create(KeyValueHeaderChangeEventFormat
						.of(
								io.debezium.engine.format.JsonByteArray.class,
								io.debezium.engine.format.JsonByteArray.class,
								io.debezium.engine.format.JsonByteArray.class))
				.using(DebeziumMySqlTestContainer.connectorConfig(DebeziumMySqlTestContainer.mysqlPort()));
	}

	@Bean
	public MessageChannel debeziumInputChannel() {
		return new DirectChannel();
	}

	@Bean
	@BridgeFrom("debeziumInputChannel")
	public MessageChannel queueChannel() {
		return new QueueChannel();
	}

}
