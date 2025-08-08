/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.outbound;

import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class JpaOutboundChannelAdapterTests {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MessageChannel jpaOutboundChannelAdapterWithinChain;

	@BeforeEach
	public void cleanUp() {
		this.jdbcTemplate.execute("delete from Student where rollNumber > 1003 or rollNumber < 1001");
	}

	@Test
	public void saveEntityWithMerge() {
		List<?> results1 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results1).isNotNull();
		assertThat(results1.size() == 3).isTrue();

		JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setEntityClass(StudentDomain.class);
		jpaExecutor.setBeanFactory(mock(BeanFactory.class));
		jpaExecutor.afterPropertiesSet();

		final JpaOutboundGateway jpaOutboundChannelAdapter = new JpaOutboundGateway(jpaExecutor);
		jpaOutboundChannelAdapter.setProducesReply(false);

		StudentDomain testStudent = JpaTestUtils.getTestStudent();
		final Message<StudentDomain> message = MessageBuilder.withPayload(testStudent).build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jpaOutboundChannelAdapter.handleMessage(message);
			}

		});

		List<?> results2 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results2).hasSize(4);

		assertThat(results2.get(0)).extracting("rollNumber").isNotNull();
	}

	@Test
	public void saveEntityWithMergeWithoutSpecifyingEntityClass() {
		List<?> results1 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results1).isNotNull();
		assertThat(results1.size() == 3).isTrue();

		JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setBeanFactory(mock(BeanFactory.class));
		jpaExecutor.afterPropertiesSet();

		final JpaOutboundGateway jpaOutboundChannelAdapter = new JpaOutboundGateway(jpaExecutor);
		jpaOutboundChannelAdapter.setProducesReply(false);

		StudentDomain testStudent = JpaTestUtils.getTestStudent();
		final Message<StudentDomain> message = MessageBuilder.withPayload(testStudent).build();

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jpaOutboundChannelAdapter.handleMessage(message);
			}

		});

		List<?> results2 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results2).hasSize(4);

		assertThat(results2.get(0)).extracting("rollNumber").isNotNull();
	}

	@Test
	public void saveEntityWithPersist() {
		List<?> results1 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results1).isNotNull();
		assertThat(results1.size() == 3).isTrue();

		JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setEntityClass(StudentDomain.class);
		jpaExecutor.setPersistMode(PersistMode.PERSIST);
		jpaExecutor.setBeanFactory(mock(BeanFactory.class));
		jpaExecutor.afterPropertiesSet();

		final JpaOutboundGateway jpaOutboundChannelAdapter = new JpaOutboundGateway(jpaExecutor);
		jpaOutboundChannelAdapter.setProducesReply(false);

		StudentDomain testStudent = JpaTestUtils.getTestStudent();

		assertThat(testStudent.getRollNumber()).isNull();

		final Message<StudentDomain> message = MessageBuilder.withPayload(testStudent).build();

		jpaOutboundChannelAdapter.setBeanFactory(mock(BeanFactory.class));
		jpaOutboundChannelAdapter.afterPropertiesSet();

		TransactionTemplate transactionTemplate = new TransactionTemplate(this.transactionManager);
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {

			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jpaOutboundChannelAdapter.handleMessage(message);
			}

		});

		List<?> results2 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results2).isNotNull();
		assertThat(results2.size() == 4).isTrue();

		assertThat(testStudent.getRollNumber()).isNotNull();
	}

	@Test
	public void saveEntityWithPersistWithinChain() {
		List<?> results1 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results1).isNotNull();
		assertThat(results1.size() == 3).isTrue();

		StudentDomain testStudent = JpaTestUtils.getTestStudent();

		assertThat(testStudent.getRollNumber()).isNull();

		this.jpaOutboundChannelAdapterWithinChain.send(MessageBuilder.withPayload(testStudent).build());

		List<?> results2 = this.jdbcTemplate.queryForList("Select * from Student");
		assertThat(results2).isNotNull();
		assertThat(results2.size() == 4).isTrue();

		assertThat(testStudent.getRollNumber()).isNotNull();
	}

}
