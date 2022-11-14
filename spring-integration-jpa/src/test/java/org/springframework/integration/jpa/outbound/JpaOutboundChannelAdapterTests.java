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
		assertThat(results2).isNotNull();
		assertThat(results2.size() == 4).isTrue();

		assertThat(testStudent.getRollNumber()).isNull();

	}

	@Test
	public void saveEntityWithMergeWithoutSpecifyingEntityClass() throws InterruptedException {

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
		assertThat(results2).isNotNull();
		assertThat(results2.size() == 4).isTrue();

		assertThat(testStudent.getRollNumber()).isNull();

	}

	@Test
	public void saveEntityWithPersist() throws InterruptedException {

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

	@Test //INT-2557
	public void saveEntityWithPersistWithinChain() throws InterruptedException {

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
