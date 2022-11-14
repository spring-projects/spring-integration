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

package org.springframework.integration.jpa.outbound;

import java.util.List;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class JpaOutboundChannelAdapterTransactionalTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel channel;

	@Autowired
	DataSource dataSource;

	@Test
	public void saveEntityWithTransaction() {
		List<?> results1 =
				new JdbcTemplate(this.dataSource)
						.queryForList("Select * from Student");
		assertThat(results1).isNotNull();
		assertThat(results1.size()).isEqualTo(3);

		StudentDomain testStudent = JpaTestUtils.getTestStudent();
		Message<StudentDomain> message = MessageBuilder.withPayload(testStudent).build();

		this.channel.send(message);

		List<?> results2 =
				new JdbcTemplate(this.dataSource)
						.queryForList("Select * from Student");
		assertThat(results2).isNotNull();
		assertThat(results2.size()).isEqualTo(4);

		assertThat(testStudent.getRollNumber()).isNull();
	}

}
