/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filippo Balicchia
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SpringJUnitConfig
class CassandraOutboundAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void minimalConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outbound1.adapter"), "handler",
						CassandraMessageHandler.class);

		assertThat(TestUtils.getPropertyValue(handler, "componentName")).isEqualTo("outbound1.adapter");
		assertThat(TestUtils.getPropertyValue(handler, "mode")).isEqualTo(CassandraMessageHandler.Type.INSERT);
		assertThat(TestUtils.getPropertyValue(handler, "cassandraOperations"))
				.isSameAs(this.context.getBean("cassandraTemplate"));
		assertThat(TestUtils.getPropertyValue(handler, "writeOptions")).isSameAs(this.context.getBean("writeOptions"));
		assertThat(TestUtils.getPropertyValue(handler, "async", Boolean.class)).isFalse();
	}

	@Test
	void ingestConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outbound2"), "handler",
						CassandraMessageHandler.class);

		assertThat(TestUtils.getPropertyValue(handler, "ingestQuery"))
				.isEqualTo("insert into book (isbn, title, author, pages, saleDate, isInStock) " +
						"values (?, ?, ?, ?, ?, ?)");
		assertThat(TestUtils.getPropertyValue(handler, "producesReply", Boolean.class)).isFalse();
	}

	@Test
	void fullConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outgateway"), "handler",
						CassandraMessageHandler.class);

		assertThat(TestUtils.getPropertyValue(handler, "producesReply", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(handler, "mode")).isEqualTo(CassandraMessageHandler.Type.STATEMENT);
		assertThat(TestUtils.getPropertyValue(handler, "writeOptions")).isSameAs(this.context.getBean("writeOptions"));
	}

	@Test
	void statementConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outbound4.adapter"), "handler",
						CassandraMessageHandler.class);

		assertThat(TestUtils.getPropertyValue(handler, "componentName")).isEqualTo("outbound4.adapter");
		assertThat(TestUtils.getPropertyValue(handler, "mode")).isEqualTo(CassandraMessageHandler.Type.STATEMENT);
		assertThat(TestUtils.getPropertyValue(handler, "cassandraOperations"))
				.isSameAs(this.context.getBean("cassandraTemplate"));
		assertThat(TestUtils.getPropertyValue(handler, "writeOptions")).isSameAs(this.context.getBean("writeOptions"));
	}

}
