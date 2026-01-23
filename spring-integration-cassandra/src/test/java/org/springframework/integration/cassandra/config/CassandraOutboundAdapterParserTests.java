/*
 * Copyright 2022-present the original author or authors.
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
				TestUtils.<CassandraMessageHandler>getPropertyValue(this.context.getBean("outbound1.adapter"), "handler");

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
				TestUtils.<CassandraMessageHandler>getPropertyValue(this.context.getBean("outbound2"), "handler");

		assertThat(TestUtils.<String>getPropertyValue(handler, "ingestQuery"))
				.isEqualTo("insert into book (isbn, title, author, pages, saleDate, isInStock) " +
						"values (?, ?, ?, ?, ?, ?)");
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "producesReply")).isFalse();
	}

	@Test
	void fullConfig() {
		CassandraMessageHandler handler =
				TestUtils.<CassandraMessageHandler>getPropertyValue(this.context.getBean("outgateway"), "handler");

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
