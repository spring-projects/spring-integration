/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jpa.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.test.Consumer;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the Jpa Polling Channel Adapter {@link JpaPollingChannelAdapter}.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@Rollback
@Transactional("transactionManager")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class JpaPollingChannelAdapterTests {

	@Autowired
	private GenericApplicationContext context;

	@Autowired
	EntityManager entityManager;

	@Autowired
	JpaOperations jpaOperations;

	@Autowired
	@Qualifier("jpaPoller")
	PollerMetadata poller;

	@Autowired
	@Qualifier("outputChannel")
	private MessageChannel outputChannel;

	@Autowired
	OnlyOnceTrigger testTrigger;

	/**
	 * In this test, a Jpa Polling Channel Adapter will use a plain entity class
	 * to retrieve a list of records from the database.
	 */
	@Test
	public void testWithEntityClass() throws Exception {
		testTrigger.reset();
		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setEntityClass(StudentDomain.class);
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(10000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Collection<?> primeNumbers = message.getPayload();

		assertThat(primeNumbers.size()).isEqualTo(3);
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database.
	 */
	@Test
	public void testWithJpaQuery() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student");
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(10000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Collection<?> primeNumbers = message.getPayload();

		assertThat(primeNumbers.size()).isEqualTo(3);
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database with a maxRows value of 1.
	 */
	@Test
	public void testWithJpaQueryAndMaxResults() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student");
		jpaExecutor.setMaxResultsExpression(new LiteralExpression("1"));
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(10000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Collection<?> primeNumbers = message.getPayload();

		assertThat(primeNumbers.size()).isEqualTo(1);
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database.
	 */
	@Test
	public void testWithJpaQueryOneResultOnly() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s where s.firstName = 'First Two'");
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();
		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(10000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Collection<?> students = message.getPayload();

		assertThat(students.size()).isEqualTo(1);

		StudentDomain student = (StudentDomain) students.iterator().next();

		assertThat(student.getLastName()).isEqualTo("Last Two");
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database. Additionally, the records
	 * will be deleted after the polling.
	 */
	@Test
	public void testWithJpaQueryAndDelete() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s");
		jpaExecutor.setDeleteAfterPoll(true);
		jpaExecutor.setDeleteInBatch(true);
		jpaExecutor.setFlush(true);
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(10000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).as("Message is null.").isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Collection<?> students = message.getPayload();

		assertThat(students.size()).isEqualTo(3);

		await().until(() -> entityManager.createQuery("select count(*) from Student", Long.class).getSingleResult(),
				(count) -> count == 0);
	}

	@Test
	public void testWithJpaQueryButNoResultsAndDelete() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s where s.lastName = 'Something Else'");
		jpaExecutor.setDeleteAfterPoll(true);
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		final Consumer consumer = new Consumer();

		final List<Message<Collection<?>>> received = new ArrayList<>();
		received.add(consumer.poll(100));

		final Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).isNull();
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database. Additionally, the records
	 * will be deleted after the polling.
	 */
	@Test
	public void testWithJpaQueryAndDeletePerRow() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(jpaOperations);
		jpaExecutor.setJpaQuery("from Student s");
		jpaExecutor.setDeleteAfterPoll(true);
		jpaExecutor.setDeleteInBatch(false);
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		final Consumer consumer = new Consumer();

		final List<Message<Collection<?>>> received = new ArrayList<>();
		received.add(consumer.poll(10000));

		final Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).as("Message is null.").isNotNull();
		assertThat(message.getPayload()).isNotNull();

		final Collection<?> students = message.getPayload();

		assertThat(students.size()).isEqualTo(3);

		await().until(() -> entityManager.createQuery("select count(*) from Student", Long.class).getSingleResult(),
				(count) -> count == 0);
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use a Native SQL query
	 * to retrieve a list of records from the database.
	 */
	@Test
	public void testWithNativeSqlQuery() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setNativeQuery("select * from Student where lastName = 'Last One'");
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(10000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Collection<?> students = message.getPayload();

		assertThat(students.size()).isEqualTo(1);
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use Named query
	 * to retrieve a list of records from the database.
	 */
	@Test
	public void testWithNamedQuery() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setNamedQuery("selectStudent");
		jpaExecutor.setBeanFactory(this.context);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(10000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isNotNull();

		Collection<?> students = message.getPayload();

		assertThat(students.size()).isEqualTo(1);
	}

}
