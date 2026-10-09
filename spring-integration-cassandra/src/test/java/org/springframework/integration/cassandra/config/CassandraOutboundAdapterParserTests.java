/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filippo Balicchia
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
class CassandraOutboundAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void minimalConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outbound1.adapter"), "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "componentName")).isEqualTo("outbound1.adapter");
		assertThat(TestUtils.<CassandraMessageHandler.Type>getPropertyValue(handler, "mode"))
				.isEqualTo(CassandraMessageHandler.Type.INSERT);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "cassandraOperations"))
				.isSameAs(this.context.getBean("cassandraTemplate"));
		assertThat(TestUtils.<Object>getPropertyValue(handler, "writeOptions"))
				.isSameAs(this.context.getBean("writeOptions"));
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "async")).isFalse();
	}

	@Test
	void ingestConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outbound2"), "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "ingestQuery"))
				.isEqualTo("insert into book (isbn, title, author, pages, saleDate, isInStock) " +
						"values (?, ?, ?, ?, ?, ?)");
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "producesReply")).isFalse();
	}

	@Test
	void fullConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outgateway"), "handler");

		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "producesReply")).isTrue();
		assertThat(TestUtils.<CassandraMessageHandler.Type>getPropertyValue(handler, "mode"))
				.isEqualTo(CassandraMessageHandler.Type.STATEMENT);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "writeOptions"))
				.isSameAs(this.context.getBean("writeOptions"));
	}

	@Test
	void statementConfig() {
		CassandraMessageHandler handler =
				TestUtils.getPropertyValue(this.context.getBean("outbound4.adapter"), "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "componentName"))
				.isEqualTo("outbound4.adapter");
		assertThat(TestUtils.<CassandraMessageHandler.Type>getPropertyValue(handler, "mode"))
				.isEqualTo(CassandraMessageHandler.Type.STATEMENT);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "cassandraOperations"))
				.isSameAs(this.context.getBean("cassandraTemplate"));
		assertThat(TestUtils.<Object>getPropertyValue(handler, "writeOptions"))
				.isSameAs(this.context.getBean("writeOptions"));
	}

}
