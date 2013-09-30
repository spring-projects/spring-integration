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
package org.springframework.integration.jpa.core;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.jpa.support.parametersource.BeanPropertyParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;
import org.springframework.util.Assert;

/**
 * Executes Jpa Operations that produce payload objects from the result of the provided:
 *
 * <ul>
 *     <li>entityClass</li>
 *     <li>JpQl Select Query</li>
 *     <li>Sql Native Query</li>
 *     <li>JpQl Named Query</li>
 *     <li>Sql Native Named Query</li>
 * </ul>
 *
 * When objects are being retrieved, it also possibly to:
 *
 * <ul>
 *     <li>delete the retrieved object</li>
 * </ul>
 *
 * If neither entityClass nor any other query is specified then the entity-class
 * is "guessed" from the {@link Message} payload.
 *
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @since 2.2
 *
 */
public class JpaExecutor implements InitializingBean, BeanFactoryAware, IntegrationEvaluationContextAware {

	private volatile JpaOperations      jpaOperations;
	private volatile List<JpaParameter> jpaParameters;

	private volatile Class<?> entityClass;
	private volatile String   jpaQuery;
	private volatile String   nativeQuery;
	private volatile String   namedQuery;

	private volatile Expression maxNumberOfResultsExpression;

	private volatile Expression firstResultExpression;

	private volatile PersistMode persistMode = PersistMode.MERGE;

	private volatile ParameterSourceFactory parameterSourceFactory = null;
	private volatile ParameterSource parameterSource;

	private volatile boolean  deleteAfterPoll = false;
	private volatile boolean  deleteInBatch = false;

	private volatile boolean  expectSingleResult = false;

	/**
	 * Indicates that whether only the payload of the passed in {@link Message}
	 * will be used as a source of parameters. The is 'true' by default because as a
	 * default a {@link BeanPropertyJpaParameterSourceFactory} implementation is
	 * used for the sqlParameterSourceFactory property.
	 */
	private volatile Boolean usePayloadAsParameterSource = null;

	private volatile BeanFactory beanFactory;

	private volatile EvaluationContext evaluationContext;

	/**
	 * Constructor taking an {@link EntityManagerFactory} from which the
	 * {@link EntityManager} can be obtained.
	 *
	 * @param entityManagerFactory Must not be null.
	 */
	public JpaExecutor(EntityManagerFactory entityManagerFactory) {
		Assert.notNull(entityManagerFactory, "entityManagerFactory must not be null.");

		DefaultJpaOperations defaultJpaOperations = new DefaultJpaOperations();
		defaultJpaOperations.setEntityManagerFactory(entityManagerFactory);
		defaultJpaOperations.afterPropertiesSet();

		this.jpaOperations = defaultJpaOperations;
	}

	/**
	 * Constructor taking an {@link EntityManager} directly.
	 *
	 * @param entityManager Must not be null.
	 */
	public JpaExecutor(EntityManager entityManager) {
		Assert.notNull(entityManager, "entityManager must not be null.");

		DefaultJpaOperations defaultJpaOperations = new DefaultJpaOperations();
		defaultJpaOperations.setEntityManager(entityManager);
		defaultJpaOperations.afterPropertiesSet();
		this.jpaOperations = defaultJpaOperations;
	}

	/**
	 * If custom behavior is required a custom implementation of {@link JpaOperations}
	 * can be passed in. The implementations themselves typically provide access
	 * to the {@link EntityManager}.
	 *
	 * See also {@link DefaultJpaOperations} and {@link AbstractJpaOperations}.
	 *
	 * @param jpaOperations Must not be null.
	 */
	public JpaExecutor(JpaOperations jpaOperations) {
		Assert.notNull(jpaOperations, "jpaOperations must not be null.");
		this.jpaOperations = jpaOperations;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 *
	 * Verifies and sets the parameters. E.g. initializes the to be used
	 * {@link ParameterSourceFactory}.
	 *
	 */
	@Override
	public void afterPropertiesSet() {

		if (this.jpaParameters != null) {

			if (this.parameterSourceFactory == null) {
				ExpressionEvaluatingParameterSourceFactory expressionSourceFactory =
							  new ExpressionEvaluatingParameterSourceFactory(this.beanFactory);
				expressionSourceFactory.setParameters(jpaParameters);
				this.parameterSourceFactory = expressionSourceFactory;

			}
			else {

				if (!(this.parameterSourceFactory instanceof ExpressionEvaluatingParameterSourceFactory)) {
					throw new IllegalStateException("You are providing 'JpaParameters'. "
						+ "Was expecting the the provided jpaParameterSourceFactory "
						+ "to be an instance of 'ExpressionEvaluatingJpaParameterSourceFactory', "
						+ "however the provided one is of type '" + this.parameterSourceFactory.getClass().getName() + "'");
				}

			}

			if (this.usePayloadAsParameterSource == null) {
				this.usePayloadAsParameterSource = false;
			}

		}
		else {

			if (this.parameterSourceFactory == null) {
				this.parameterSourceFactory = new BeanPropertyParameterSourceFactory();
			}

			if (this.usePayloadAsParameterSource == null) {
				this.usePayloadAsParameterSource = true;
			}

		}

		if(maxNumberOfResultsExpression == null) {
			maxNumberOfResultsExpression = new LiteralExpression("0");
		}

	}

	/**
	 * Executes the actual Jpa Operation. Call this method, if you need access to
	 * process return values. This methods return a Map that contains either
	 * the number of affected entities or the affected entity itself.
	 *
	 * Keep in mind that the number of entities effected by the operation may
	 * not necessarily correlate with the number of rows effected in the database.
	 *
	 * @param message
	 * @return Either the number of affected entities when using a JPAQL query. When using a merge/persist the updated/inserted itself is returned.
	 */
	public Object executeOutboundJpaOperation(final Message<?> message) {

		final Object result;

		ParameterSource parameterSource = null;
		if (this.jpaQuery != null || this.nativeQuery != null || this.namedQuery != null) {
			parameterSource = determineParameterSource(message);
		}
		if (this.jpaQuery != null) {

			result = this.jpaOperations.executeUpdate(this.jpaQuery, parameterSource);

		}
		else if (this.nativeQuery != null) {

			result = this.jpaOperations.executeUpdateWithNativeQuery(this.nativeQuery, parameterSource);

		}
		else if (this.namedQuery != null) {

			result = this.jpaOperations.executeUpdateWithNamedQuery(this.namedQuery, parameterSource);

		}
		else {

			if (PersistMode.PERSIST.equals(this.persistMode)) {
				this.jpaOperations.persist(message.getPayload());
				result = message.getPayload();
			}
			else if (PersistMode.MERGE.equals(this.persistMode)) {
				final Object mergedEntity = this.jpaOperations.merge(message.getPayload());
				result = mergedEntity;
			}
			else if (PersistMode.DELETE.equals(this.persistMode)) {
				this.jpaOperations.delete(message.getPayload());
				result = message.getPayload();
			}
			else {
				throw new IllegalStateException(String.format("Unsupported PersistMode: '%s'", this.persistMode.name()));
			}

		}

		return result;

	}


	/**
	 * Execute a (typically retrieving) JPA operation. The <i>requestMessage</i>
	 * can be used to provide additional query parameters using
	 * {@link JpaExecutor#parameterSourceFactory}. If the
	 * <i>requestMessage</i> parameter is null then
	 * {@link JpaExecutor#parameterSource} is being used for providing query parameters.
	 *
	 * @param requestMessage May be null.
	 * @return The payload object, which may be null.
	 */
	@SuppressWarnings("unchecked")
	public Object poll(final Message<?> requestMessage) {
		final Object payload;
		final List<?> result;
		int maxNumberOfResults =
			evaluateExpressionForNumericResult(requestMessage, maxNumberOfResultsExpression);
		if (requestMessage == null) {
			result = doPoll(this.parameterSource, 0, maxNumberOfResults);
		}
		else {
			int firstResult = 0;
			if(firstResultExpression != null) {
				firstResult = getFirstResult(requestMessage);
			}
			ParameterSource parameterSource = determineParameterSource(requestMessage);
			result = doPoll(parameterSource, firstResult, maxNumberOfResults);
		}

		if (result.isEmpty()) {
			payload = null;
		}
		else {

			if (this.expectSingleResult) {
				if (result.size() == 1) {
					payload = result.iterator().next();
				}
				else {
					throw new MessagingException(requestMessage,
						"The Jpa operation returned more than "
					  + "1 result object but expectSingleResult was 'true'.");
				}
			}
			else {
				payload = result;
			}
		}

		if (payload != null && this.deleteAfterPoll) {
			if (payload instanceof Iterable) {
				if (this.deleteInBatch) {
					this.jpaOperations.deleteInBatch((Iterable<Object>) payload);
				}
				else {
					for (Object entity : (Iterable<?>) payload) {
						this.jpaOperations.delete(entity);
					}
				}
			}
			else {
				this.jpaOperations.delete(payload);
			}

		}
		return payload;
	}

	private int getFirstResult(final Message<?> requestMessage) {
		int firstResult = evaluateExpressionForNumericResult(requestMessage, firstResultExpression);
		return firstResult;
	}

	private int evaluateExpressionForNumericResult(
			final Message<?> requestMessage, Expression expression) {
		int firstResult = 0;
		Object evaluationResult = expression.getValue(evaluationContext, requestMessage);
		if(evaluationResult != null) {
			if(evaluationResult instanceof Number) {
				firstResult = ((Number)evaluationResult).intValue();
			}
			else if(evaluationResult instanceof String){
				try {
					firstResult = Integer.parseInt((String)evaluationResult);
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException(
							"Value " + evaluationResult + " passed as cannot be " +
									"parsed to a number, expected to be numeric");
				}
			}
			else {
				throw new IllegalArgumentException("Expected the value to be a Number" +
						     " got " + evaluationResult.getClass().getName());
			}
		}
		return firstResult;
	}

	private ParameterSource determineParameterSource(final Message<?> requestMessage) {
		ParameterSource parameterSource;
		if (usePayloadAsParameterSource) {
			parameterSource = this.parameterSourceFactory.createParameterSource(requestMessage.getPayload());
		}
		else {
			parameterSource = this.parameterSourceFactory.createParameterSource(requestMessage);
		}
		return parameterSource;
	}

	/**
	 * Execute the JPA operation. Delegates to {@link JpaExecutor#poll(Message)}.
	 */
	public Object poll() {
		return poll(null);
	}

	protected List<?> doPoll(ParameterSource jpaQLParameterSource, int firstResult,
			int maxNumberOfResults) {
		List<?> payload = null;
		if (this.jpaQuery != null) {
			payload = jpaOperations.getResultListForQuery(this.jpaQuery, jpaQLParameterSource,
							firstResult, maxNumberOfResults);
		}
		else if (this.nativeQuery != null) {
			payload = jpaOperations.getResultListForNativeQuery(this.nativeQuery, this.entityClass, jpaQLParameterSource,
							firstResult, maxNumberOfResults);
		}
		else if (this.namedQuery != null) {
			payload = jpaOperations.getResultListForNamedQuery(this.namedQuery, jpaQLParameterSource,
							firstResult, maxNumberOfResults);
		}
		else if (this.entityClass != null) {
			payload = jpaOperations.getResultListForClass(this.entityClass,
							firstResult, maxNumberOfResults);
		}
		else {
			throw new IllegalStateException("For the polling operation, one of "
								+ "the following properties must be specified: "
								+ "query, namedQuery or entityClass.");
		}
		return payload;
	}

	/**
	 * Sets the class type which is being used for retrieving entities from the
	 * database.
	 *
	 * @param entityClass Must not be null.
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "entityClass must not be null.");
		this.entityClass = entityClass;
	}

	/**
	 * @param jpaQuery The provided JPA query must neither be null nor empty.
	 */
	public void setJpaQuery(String jpaQuery) {
		Assert.isTrue(this.nativeQuery == null && this.namedQuery == null, "You can define only one of the "
							+ "properties 'jpaQuery', 'nativeQuery', 'namedQuery'");
		Assert.hasText(jpaQuery, "jpaQuery must neither be null nor empty.");
		this.jpaQuery = jpaQuery;
	}

	/**
	 * You can also use native Sql queries to poll data from the database. If set
	 * this property will allow you to use native SQL. Optionally you can also set
	 * the entityClass property at the same time. If specified the entityClass will
	 * be used as the result class for the native query.
	 *
	 * @param nativeQuery The provided SQL query must neither be null nor empty.
	 */
	public void setNativeQuery(String nativeQuery) {

		Assert.isTrue(this.namedQuery == null && this.jpaQuery == null, "You can define only one of the "
				+ "properties 'jpaQuery', 'nativeQuery', 'namedQuery'");;
		Assert.hasText(nativeQuery, "nativeQuery must neither be null nor empty.");

		this.nativeQuery = nativeQuery;
	}

	/**
	 * A named query can either refer to a named JPQL based query or a native SQL
	 * query.
	 *
	 * @param namedQuery Must neither be null nor empty
	 */
	public void setNamedQuery(String namedQuery) {

		Assert.isTrue(this.jpaQuery == null && this.nativeQuery == null, "You can define only one of the "
				+ "properties 'jpaQuery', 'nativeQuery', 'namedQuery'");

		Assert.hasText(namedQuery, "namedQuery must neither be null nor empty.");
		this.namedQuery = namedQuery;
	}

	public void setPersistMode(PersistMode persistMode) {
		this.persistMode = persistMode;
	}

	public void setJpaParameters(List<JpaParameter> jpaParameters) {
		this.jpaParameters = jpaParameters;
	}

	public void setUsePayloadAsParameterSource(Boolean usePayloadAsParameterSource) {
		this.usePayloadAsParameterSource = usePayloadAsParameterSource;
	}

	/**
	 * If not set, this property defaults to <code>false</code>, which means that
	 * deletion occurs on a per object basis if a collection of entities is being
	 * deleted.
	 *
	 * If set to 'true' the elements of the payload are deleted as a batch
	 * operation. Be aware that this exhibits issues in regards to cascaded deletes.
	 *
	 * The specification 'JSR 317: Java Persistence API, Version 2.0' does not
	 * support cascaded deletes in batch operations. The specification states in
	 * chapter 4.10:
	 *
	 * "A delete operation only applies to entities of the specified class and
	 * its subclasses. It does not cascade to related entities."
	 *
	 * @param deleteInBatch Defaults to 'false' if not set.
	 */
	public void setDeleteInBatch(boolean deleteInBatch) {
		this.deleteInBatch = deleteInBatch;
	}

	/**
	 * If set to 'true', the retrieved objects are deleted from the database upon
	 * being polled. May not work in all situations, e.g. for Native SQL Queries.
	 *
	 * @param deleteAfterPoll Defaults to 'false'.
	 */
	public void setDeleteAfterPoll(boolean deleteAfterPoll) {
		this.deleteAfterPoll = deleteAfterPoll;
	}

	/**
	 *
	 * @param parameterSourceFactory Must not be null
	 */
	public void setParameterSourceFactory(
			ParameterSourceFactory parameterSourceFactory) {
		Assert.notNull(parameterSourceFactory, "parameterSourceFactory must not be null.");
		this.parameterSourceFactory = parameterSourceFactory;
	}

	/**
	 * Specifies the {@link ParameterSource} that would be used to provide
	 * additional parameters.
	 *
	 * @param parameterSource Must not be null.
	 */
	public void setParameterSource(ParameterSource parameterSource) {
		Assert.notNull(parameterSource, "parameterSource must not be null.");
		this.parameterSource = parameterSource;
	}

	/**
	 *
	 * This parameter indicates that only one result object shall be returned as
	 * a result from the executed JPA operation. If set to <code>true</code> and
	 * the result list from the JPA operations contains only 1 element, then that
	 * 1 element is extracted and returned as payload.
	 *
	 * If the result map contains more than 1 element and
	 * {@link JpaExecutor#expectSingleResult} is <code>true</code>, then a
	 * {@link MessagingException} is thrown.
	 *
	 * If set to <code>false</code>, the complete result list is returned as the
	 * payload.
	 *
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Sets the expression that will be evaluated to get the first result in the query executed.
	 * If a null expression is set, all the results in the result set will be retrieved
	 *
	 * @param firstResultExpression
	 *
	 * @see Query#setFirstResult(int)
	 */
	public void setFirstResultExpression(Expression firstResultExpression) {
		this.firstResultExpression = firstResultExpression;
	}



	/**
	 * Sets the expression for maximum number of results expression. It has be a non null value
	 * Not setting one will default to the behavior of fetching all the records
	 *
	 * @param maxNumberOfResultsExpression
	 */
	public void setMaxNumberOfResultsExpression(
			Expression maxNumberOfResultsExpression) {
		Assert.notNull(maxNumberOfResultsExpression, "maxNumberOfResultsExpression cannot be null");
		this.maxNumberOfResultsExpression = maxNumberOfResultsExpression;
	}

	/**
	 * Sets the evaluation context for evaluating the expression to get the from record of the
	 * result set retrieved by the retrieving gateway.
	 *
	 * @param evaluationContext
	 */
	@Override
	public void setIntegrationEvaluationContext(
			EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}
}
