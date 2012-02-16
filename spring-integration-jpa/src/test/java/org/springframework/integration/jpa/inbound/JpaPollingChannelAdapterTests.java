/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.test.Consumer;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.TestTrigger;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the Jpa Polling Channel Adapter {@link JpaPollingChannelAdapter}.
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=true)
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
	TestTrigger testTrigger;

	/**
	 * In this test, a Jpa Polling Channel Adapter will use a plain entity class
	 * to retrieve a list of records from the database.
	 *
	 * @throws Exception
	 */
	@Test
	@DirtiesContext
	public void testWithEntityClass() throws Exception {
		testTrigger.reset();
		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setEntityClass(StudentDomain.class);

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> primeNumbers = message.getPayload();

		assertTrue(primeNumbers.size() == 3);

	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWithJpaQuery() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student");

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> primeNumbers = message.getPayload();

		assertTrue(primeNumbers.size() == 3);

	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database with a maxRows value of 1.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWithJpaQueryAndMaxResults() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student");
		jpaExecutor.setMaxRows(1);

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> primeNumbers = message.getPayload();

		assertTrue(primeNumbers.size() == 1);

	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWithJpaQueryOneResultOnly() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s where s.firstName = 'First Two'");

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> students = message.getPayload();

		assertTrue(students.size() == 1);

		StudentDomain student = (StudentDomain) students.iterator().next();

		assertEquals("Last Two", student.getLastName());
	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database. Additionaly, the records
	 * will be deleted after the polling.
	 *
	 * @throws Exception
	 */
	@Test
	@DirtiesContext
	@Transactional
	public void testWithJpaQueryAndDelete() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s");
		jpaExecutor.setDeleteAfterPoll(true);

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull("Message is null.", message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> students = message.getPayload();

		assertTrue(students.size() == 3);

		Long studentCount = entityManager.createQuery("select count(*) from Student", Long.class).getSingleResult();

		assertEquals(Long.valueOf(0), studentCount);

	}

	@Test
	@DirtiesContext
	@Transactional
	public void testWithJpaQueryButNoResultsAndDelete() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s where s.lastName = 'Something Else'");
		jpaExecutor.setDeleteAfterPoll(true);

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNull(message);

	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use JpQL query
	 * to retrieve a list of records from the database. Additionaly, the records
	 * will be deleted after the polling.
	 *
	 * @throws Exception
	 */
	@Test
	@DirtiesContext
	public void testWithJpaQueryAndDeletePerRow() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(jpaOperations);
		jpaExecutor.setJpaQuery("from Student s");
		jpaExecutor.setDeleteAfterPoll(true);
		jpaExecutor.setDeletePerRow(true);

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		//adapter.stop();

		assertNotNull("Message is null.", message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> students = message.getPayload();

		assertTrue(students.size() == 3);

		Long studentCount = entityManager.createQuery("select count(*) from Student", Long.class).getSingleResult();

		assertEquals(Long.valueOf(0), studentCount);

	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use a Native SQL query
	 * to retrieve a list of records from the database.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWithNativeSqlQuery() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setNativeQuery("select * from Student where lastName = 'Last One'");

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> students = message.getPayload();

		assertTrue(students.size() == 1);

	}

	/**
	 * In this test, a Jpa Polling Channel Adapter will use Named query
	 * to retrieve a list of records from the database.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWithNamedQuery() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~

		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setNamedQuery("selectStudent");

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();

		final Consumer consumer = new Consumer();

		received.add(consumer.poll(5000));

		Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull(message);
		assertNotNull(message.getPayload());
		assertNotNull(message.getPayload() instanceof Collection<?>);

		Collection<?> students = message.getPayload();

		assertTrue(students.size() == 1);

	}

}
