/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.outbound;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
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

		assertThat(results1).hasSize(3);

		StudentDomain testStudent = JpaTestUtils.getTestStudent();
		Message<StudentDomain> message = MessageBuilder.withPayload(testStudent).build();

		this.channel.send(message);

		List<?> results2 =
				new JdbcTemplate(this.dataSource)
						.queryForList("Select * from Student");

		assertThat(results2).hasSize(4);
		assertThat(results2.get(0)).extracting("rollNumber").isNotNull();
	}

}
