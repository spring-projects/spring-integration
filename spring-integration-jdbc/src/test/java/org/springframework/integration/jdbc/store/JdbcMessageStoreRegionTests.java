/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jdbc.store;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class JdbcMessageStoreRegionTests {

	private static EmbeddedDatabase dataSource;

	private JdbcTemplate jdbcTemplate;

	private JdbcMessageStore messageStore1;

	private JdbcMessageStore messageStore2;

	@BeforeClass
	public static void setupDatabase() {
		dataSource = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:/org/springframework/integration/jdbc/schema-drop-h2.sql")
				.addScript("classpath:/org/springframework/integration/jdbc/schema-h2.sql")
				.build();
	}

	@AfterClass
	public static void shutDownDatabase() {
		dataSource.shutdown();
	}

	@Before
	public void beforeTest() {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.messageStore1 = new JdbcMessageStore(dataSource);
		messageStore1.setRegion("region1");

		this.messageStore2 = new JdbcMessageStore(dataSource);
		this.messageStore2.setRegion("region2");
	}

	@After
	public void afterTest() {
		this.jdbcTemplate.execute("delete from INT_GROUP_TO_MESSAGE");
		this.jdbcTemplate.execute("delete from INT_MESSAGE");
		this.jdbcTemplate.execute("delete from INT_MESSAGE_GROUP");
	}

	@Test
	public void testVerifyMessageCount() {

		messageStore1.addMessage(MessageBuilder.withPayload("payload1").build());
		messageStore1.addMessage(MessageBuilder.withPayload("payload2").build());

		messageStore2.addMessage(MessageBuilder.withPayload("payload1").build());
		messageStore2.addMessage(MessageBuilder.withPayload("payload2").build());

		assertThat(messageStore1.getMessageCount()).isEqualTo(2);
		assertThat(messageStore2.getMessageCount()).isEqualTo(2);

	}

	@Test
	public void testInsertNullRegion() {

		try {
			messageStore1.setRegion(null);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("Region must not be null or empty.");
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testVerifyMessageGroupCount() {

		messageStore1.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore1.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		messageStore2.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore2.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		assertThat(messageStore1.getMessageGroup("group1").getMessages().size()).isEqualTo(1);
		assertThat(messageStore2.getMessageGroup("group1").getMessages().size()).isEqualTo(1);
		assertThat(messageStore1.getMessageGroup("group2").getMessages().size()).isEqualTo(1);
		assertThat(messageStore2.getMessageGroup("group2").getMessages().size()).isEqualTo(1);

		assertThat(messageStore1.getMessageCount()).isEqualTo(2);
		assertThat(messageStore2.getMessageCount()).isEqualTo(2);

	}

	@Test
	public void testRegionSetToMessageGroup() {

		messageStore1.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());

		List<String> regions = jdbcTemplate.query("Select * from INT_MESSAGE_GROUP where REGION = 'region1'",
				(rs, rowNum) -> rs.getString("REGION"));

		assertThat(regions.size()).isEqualTo(1);
		assertThat(regions.get(0)).isEqualTo("region1");

		messageStore2.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());

		List<String> regions2 = jdbcTemplate.query("Select * from INT_MESSAGE_GROUP where REGION = 'region2'",
				(rs, rowNum) -> rs.getString("REGION"));

		assertThat(regions2.size()).isEqualTo(1);
		assertThat(regions2.get(0)).isEqualTo("region2");

	}

	@Test
	public void testRemoveMessageGroup() {

		messageStore1.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore1.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		messageStore2.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore2.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		messageStore1.removeMessageGroup("group1");

		assertThat(messageStore1.getMessageGroupCount()).isEqualTo(1);
		assertThat(messageStore2.getMessageGroupCount()).isEqualTo(2);

	}

}
