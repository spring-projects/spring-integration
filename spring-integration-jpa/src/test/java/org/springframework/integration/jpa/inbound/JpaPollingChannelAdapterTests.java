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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.test.Consumer;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.TestTrigger;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

/**
 * Integration tests for the Jpa Polling Channel Adapter {@link JpaPollingChannelAdapter}.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
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
		jpaExecutor.afterPropertiesSet();

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
		assertTrue(message.getPayload() instanceof Collection<?>);

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
		jpaExecutor.afterPropertiesSet();

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
		assertTrue(message.getPayload() instanceof Collection<?>);

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
		jpaExecutor.setMaxResultsExpression(new LiteralExpression("1"));
		jpaExecutor.afterPropertiesSet();

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
		assertTrue(message.getPayload() instanceof Collection<?>);

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
		jpaExecutor.afterPropertiesSet();
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
		assertTrue(message.getPayload() instanceof Collection<?>);

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
	public void testWithJpaQueryAndDelete() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s");
		jpaExecutor.setDeleteAfterPoll(true);
		jpaExecutor.setDeleteInBatch(true);
		jpaExecutor.setFlush(true);
		jpaExecutor.afterPropertiesSet();

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
		assertTrue(message.getPayload() instanceof Collection<?>);

		Collection<?> students = message.getPayload();

		assertTrue(students.size() == 3);

		assertEquals(Long.valueOf(0), entityManager.createQuery("select count(*) from Student", Long.class).getSingleResult());

	}

	private Long waitForDeletes(Collection<?> students) throws InterruptedException {
		Long studentCount = Long.valueOf(students.size());

		int n = 0;

		while (studentCount > 0) {
			studentCount = entityManager.createQuery("select count(*) from Student", Long.class).getSingleResult();
			if (studentCount > 0) {
				Thread.sleep(100);
				if (n++ > 100) {
					fail("Failed to delete after poll");
				}
			}
		}
		return studentCount;
	}

	@Test
	@DirtiesContext
	public void testWithJpaQueryButNoResultsAndDelete() throws Exception {
		testTrigger.reset();

		//~~~~SETUP~~~~~
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("from Student s where s.lastName = 'Something Else'");
		jpaExecutor.setDeleteAfterPoll(true);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		Thread.sleep(1000);

		final Consumer consumer = new Consumer();

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();
		received.add(consumer.poll(5000));

		final Message<Collection<?>> message = received.get(0);

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
		jpaExecutor.setDeleteInBatch(false);
		jpaExecutor.afterPropertiesSet();

		final JpaPollingChannelAdapter jpaPollingChannelAdapter = new JpaPollingChannelAdapter(jpaExecutor);

		final SourcePollingChannelAdapter adapter = JpaTestUtils.getSourcePollingChannelAdapter(
				jpaPollingChannelAdapter, this.outputChannel, this.poller, this.context, this.getClass().getClassLoader());
		adapter.start();

		Thread.sleep(1000);

		final Consumer consumer = new Consumer();

		final List<Message<Collection<?>>> received = new ArrayList<Message<Collection<?>>>();
		received.add(consumer.poll(5000));

		final Message<Collection<?>> message = received.get(0);

		adapter.stop();

		assertNotNull("Message is null.", message);
		assertNotNull(message.getPayload());
		assertTrue(message.getPayload() instanceof Collection<?>);

		final Collection<?> students = message.getPayload();

		assertTrue(students.size() == 3);

		Long studentCount = waitForDeletes(students);

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
		jpaExecutor.afterPropertiesSet();

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
		assertTrue(message.getPayload() instanceof Collection<?>);

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
		jpaExecutor.afterPropertiesSet();

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
		assertTrue(message.getPayload() instanceof Collection<?>);

		Collection<?> students = message.getPayload();

		assertTrue(students.size() == 1);

	}

}
