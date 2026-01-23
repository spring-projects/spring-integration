/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jpa.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.2
 */
@SpringJUnitConfig
@DirtiesContext
public class JpaExecutorTests implements TestApplicationContextAware {

	@Autowired
	protected EntityManager entityManager;

	private BeanFactory beanFactory = TEST_INTEGRATION_CONTEXT;

	/**
	 * In this test, the {@link JpaExecutor}'s poll method will be called without
	 * specifying a 'query', 'namedQuery' or 'entityClass' property. This should
	 * result in an {@link IllegalArgumentException}.
	 */
	@Test
	public void testExecutePollWithNoEntityClassSpecified() {
		JpaExecutor jpaExecutor = new JpaExecutor(mock(EntityManager.class));
		jpaExecutor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		jpaExecutor.afterPropertiesSet();

		assertThatIllegalStateException()
				.isThrownBy(jpaExecutor::poll)
				.withMessage("For the polling operation, one of "
						+ "the following properties must be specified: "
						+ "jpaQuery, nativeQuery, namedQuery or entityClass.");
	}

	@Test
	public void testInstantiateJpaExecutorWithNullJpaOperations() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new JpaExecutor((JpaOperations) null))
				.withMessage("jpaOperations must not be null.");
	}

	@Test
	public void testSetMultipleQueryTypes() {
		final JpaExecutor executor = new JpaExecutor(mock(EntityManager.class));
		executor.setJpaQuery("select s from Student s");
		assertThat(TestUtils.<String>getPropertyValue(executor, "jpaQuery")).isNotNull();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> executor.setNamedQuery("NamedQuery"))
				.withMessage("Only one of the properties 'jpaQuery', 'nativeQuery', 'namedQuery' can be defined");

		assertThat(TestUtils.<Object>getPropertyValue(executor, "namedQuery")).isNull();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> executor.setNativeQuery("select * from Student"))
				.withMessage("Only one of the properties 'jpaQuery', 'nativeQuery', 'namedQuery' can be defined");

		assertThat(TestUtils.<Object>getPropertyValue(executor, "nativeQuery")).isNull();

		final JpaExecutor executor2 = new JpaExecutor(mock(EntityManager.class));
		executor2.setNamedQuery("NamedQuery");
		assertThat(TestUtils.<String>getPropertyValue(executor2, "namedQuery")).isNotNull();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> executor2.setJpaQuery("select s from Student s"))
				.withMessage("Only one of the properties 'jpaQuery', 'nativeQuery', 'namedQuery' can be defined");

		assertThat(TestUtils.<Object>getPropertyValue(executor2, "jpaQuery")).isNull();
	}

	@Test
	@Transactional
	public void selectWithMessageAsParameterSource() {
		String query = "select s from Student s where s.firstName = :firstName";
		Message<Map<String, String>> message =
				MessageBuilder.withPayload(Collections.singletonMap("firstName", "First One")).build();
		JpaExecutor executor = getJpaExecutorForMessageAsParamSource(query);
		StudentDomain student = (StudentDomain) executor.poll(message);
		assertThat(student).isNotNull();
	}

	@Test
	@Transactional
	public void selectWithPayloadAsParameterSource() {
		String query = "select s from Student s where s.firstName = :firstName";
		Message<String> message =
				MessageBuilder.withPayload("First One").build();
		JpaExecutor executor = getJpaExecutorForPayloadAsParamSource(query);
		StudentDomain student = (StudentDomain) executor.poll(message);
		assertThat(student).isNotNull();
	}

	@Test
	@Transactional
	public void updateWithMessageAsParameterSource() {
		String query = "update Student s set s.firstName = :firstName where s.lastName = 'Last One'";
		Message<Map<String, String>> message =
				MessageBuilder.withPayload(Collections.singletonMap("firstName", "First One")).build();
		JpaExecutor executor = getJpaExecutorForMessageAsParamSource(query);
		Integer rowsAffected = (Integer) executor.executeOutboundJpaOperation(message);
		assertThat((int) rowsAffected).isEqualTo(1);
	}

	@Test
	@Transactional
	public void updateWithPayloadAsParameterSource() {
		String query = "update Student s set s.firstName = :firstName where s.lastName = 'Last One'";
		Message<String> message =
				MessageBuilder.withPayload("First One").build();
		JpaExecutor executor = getJpaExecutorForPayloadAsParamSource(query);
		Integer rowsAffected = (Integer) executor.executeOutboundJpaOperation(message);
		assertThat((int) rowsAffected).isEqualTo(1);
	}

	private JpaExecutor getJpaExecutorForMessageAsParamSource(String query) {
		JpaExecutor executor = new JpaExecutor(entityManager);
		ExpressionEvaluatingParameterSourceFactory factory =
				new ExpressionEvaluatingParameterSourceFactory(TEST_INTEGRATION_CONTEXT);
		factory.setParameters(
				Collections.singletonList(new JpaParameter("firstName", null, "payload['firstName']")));
		executor.setParameterSourceFactory(factory);
		executor.setJpaQuery(query);
		executor.setExpectSingleResult(true);
		executor.setUsePayloadAsParameterSource(false);
		executor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		executor.afterPropertiesSet();
		return executor;
	}

	private JpaExecutor getJpaExecutorForPayloadAsParamSource(String query) {
		JpaExecutor executor = new JpaExecutor(entityManager);
		ExpressionEvaluatingParameterSourceFactory factory =
				new ExpressionEvaluatingParameterSourceFactory(TEST_INTEGRATION_CONTEXT);
		factory.setParameters(
				Collections.singletonList(new JpaParameter("firstName", null, "#this")));
		executor.setParameterSourceFactory(factory);
		executor.setJpaQuery(query);
		executor.setExpectSingleResult(true);
		executor.setUsePayloadAsParameterSource(true);
		executor.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		executor.afterPropertiesSet();
		return executor;
	}

	@Test
	public void testResultStartingFromThirdRecordForJPAQuery() {
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("select s from Student s");
		jpaExecutor.setFirstResultExpression(new LiteralExpression("2"));
		jpaExecutor.setBeanFactory(this.beanFactory);
		jpaExecutor.afterPropertiesSet();

		List<?> results = (List<?>) jpaExecutor.poll(MessageBuilder.withPayload("").build());
		assertThat(results).isNotNull();
		assertThat(results).hasSize(1);
	}

	@Test
	public void testResultStartingFromThirdRecordForNativeQuery() {
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setJpaQuery("select s from Student s");
		jpaExecutor.setFirstResultExpression(new LiteralExpression("2"));
		jpaExecutor.setBeanFactory(this.beanFactory);
		jpaExecutor.afterPropertiesSet();

		List<?> results = (List<?>) jpaExecutor.poll(MessageBuilder.withPayload("").build());
		assertThat(results).isNotNull();
		assertThat(results).hasSize(1);
	}

	@Test
	public void testResultStartingFromThirdRecordForNamedQuery() {
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setNamedQuery("selectAllStudents");
		jpaExecutor.setFirstResultExpression(new LiteralExpression("2"));
		jpaExecutor.setBeanFactory(this.beanFactory);
		jpaExecutor.afterPropertiesSet();

		List<?> results = (List<?>) jpaExecutor.poll(MessageBuilder.withPayload("").build());
		assertThat(results).isNotNull();
		assertThat(results).hasSize(1);
	}

	@Test
	public void testResultStartingFromThirdRecordUsingEntity() {
		final JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setEntityClass(StudentDomain.class);
		jpaExecutor.setFirstResultExpression(new LiteralExpression("2"));
		jpaExecutor.setBeanFactory(this.beanFactory);
		jpaExecutor.afterPropertiesSet();

		List<?> results = (List<?>) jpaExecutor.poll(MessageBuilder.withPayload("").build());
		assertThat(results).isNotNull();
		assertThat(results.size()).isEqualTo(1);
	}

	@Test
	public void withNullMaxResultsExpression() {
		final JpaExecutor jpaExecutor = new JpaExecutor(mock(EntityManager.class));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> jpaExecutor.setMaxResultsExpression(null))
				.withMessage("maxResultsExpression cannot be null");
	}

	@Test
	public void testParameterSourceFactoryAndJpaParameters() {
		JpaExecutor executor = new JpaExecutor(this.entityManager);
		ParameterSourceFactory parameterSourceFactory = new ExpressionEvaluatingParameterSourceFactory();
		executor.setParameterSourceFactory(parameterSourceFactory);
		executor.setJpaParameters(Collections.singletonList(new JpaParameter("firstName", null, "#this")));

		assertThatIllegalStateException()
				.isThrownBy(executor::afterPropertiesSet)
				.withMessageStartingWith("The 'jpaParameters' and 'parameterSourceFactory' are mutually exclusive.");
	}

}
