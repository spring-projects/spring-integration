/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcMessageStoreRegionTests {

	@Autowired
	DataSource dataSource;

	JdbcTemplate jdbcTemplate;

	@Autowired
	@Qualifier("messageStore1")
	private JdbcMessageStore messageStore1;

	@Autowired
	@Qualifier("messageStore2")
	private JdbcMessageStore messageStore2;

	@Before
	public void init() {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@Test
	@DirtiesContext
	public void testVerifyMessageCount() throws Exception {

		messageStore1.addMessage(MessageBuilder.withPayload("payload1").build());
		messageStore1.addMessage(MessageBuilder.withPayload("payload2").build());

		messageStore2.addMessage(MessageBuilder.withPayload("payload1").build());
		messageStore2.addMessage(MessageBuilder.withPayload("payload2").build());

		assertEquals(2, messageStore1.getMessageCount());
		assertEquals(2, messageStore2.getMessageCount());

	}

	@Test
	@DirtiesContext
	public void testInsertNullRegion() throws Exception {

		try {
			messageStore1.setRegion(null);
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Region must not be null or empty.", e.getMessage());
			return;
		}

		Assert.fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	@DirtiesContext
	public void testVerifyMessageGroupCount() throws Exception {

		messageStore1.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore1.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		messageStore2.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore2.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		assertEquals(1, messageStore1.getMessageGroup("group1").getMessages().size());
		assertEquals(1, messageStore2.getMessageGroup("group1").getMessages().size());

		assertEquals(2, messageStore1.getMessageCount());
		assertEquals(2, messageStore2.getMessageCount());

	}

	@Test
	@DirtiesContext
	public void testRegionSetToMessageGroup() throws Exception {

		messageStore1.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());

		List<String> regions = jdbcTemplate.query("Select * from INT_MESSAGE_GROUP where REGION = 'region1'", new RowMapper<String>(){

			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString("REGION");
			}

		});

		assertEquals(1, regions.size());
		assertEquals("region1", regions.get(0));

		messageStore2.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());

		List<String> regions2 = jdbcTemplate.query("Select * from INT_MESSAGE_GROUP where REGION = 'region2'", new RowMapper<String>(){

			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString("REGION");
			}

		});

		assertEquals(1, regions2.size());
		assertEquals("region2", regions2.get(0));

	}

	@Test
	@DirtiesContext
	public void testRemoveMessageGroup() throws Exception {

		messageStore1.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore1.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		messageStore2.addMessageToGroup("group1", MessageBuilder.withPayload("payload1").build());
		messageStore2.addMessageToGroup("group2", MessageBuilder.withPayload("payload2").build());

		messageStore1.removeMessageGroup("group1");

		assertEquals(1, messageStore1.getMessageGroupCount());
		assertEquals(2, messageStore2.getMessageGroupCount());

	}
}
