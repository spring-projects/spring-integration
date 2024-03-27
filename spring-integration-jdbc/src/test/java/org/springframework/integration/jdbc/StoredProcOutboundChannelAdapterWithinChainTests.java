/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jdbc;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.storedproc.User;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 2.2
 */
@RunWith(SpringRunner.class)
@DirtiesContext // close at the end after class
public class StoredProcOutboundChannelAdapterWithinChainTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MessageChannel jdbcStoredProcOutboundChannelAdapterWithinChain;

	@Test
	public void test() {
		Message<User> message = MessageBuilder.withPayload(new User("username", "password", "email")).build();
		this.jdbcStoredProcOutboundChannelAdapterWithinChain.send(message);

		Map<String, Object> map = this.jdbcTemplate.queryForMap("SELECT * FROM USERS WHERE USERNAME=?", "username");

		assertThat(map.get("USERNAME")).as("Wrong username").isEqualTo("username");
		assertThat(map.get("PASSWORD")).as("Wrong password").isEqualTo("password");
		assertThat(map.get("EMAIL")).as("Wrong email").isEqualTo("email");
	}

}
