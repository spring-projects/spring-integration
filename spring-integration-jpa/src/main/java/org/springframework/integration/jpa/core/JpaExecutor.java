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

package org.springframework.integration.jpa.core;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.jpa.support.parametersource.BeanPropertyParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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
 * is "guessed" from the {@link Message} payload.idExpression
 *
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class JpaExecutor implements InitializingBean, BeanFactoryAware {

	private final JpaOperations jpaOperations;

	private List<JpaParameter> jpaParameters;

	private Class<?> entityClass;

	private String jpaQuery;

	private String nativeQuery;

	private String namedQuery;

	private Expression maxResultsExpression;

	private Expression firstResultExpression;

	private Expression idExpression;

	private PersistMode persistMode = PersistMode.MERGE;

	private ParameterSourceFactory parameterSourceFactory = null;

	private ParameterSource parameterSource;

	private boolean flush = false;

	private int flushSize = 0;

	private boolean clearOnFlush = false;

	private boolean deleteAfterPoll = false;

	private boolean deleteInBatch = false;

	private boolean expectSingleResult = false;

	/**
	 * Indicates that whether only the payload of the passed in {@link Message}
	 * will be used as a source of parameters. The is 'true' by default because as a
	 * default a {@link BeanPropertyParameterSourceFactory} implementation is
	 * used for the sqlParameterSourceFactory property.
	 */
	private Boolean usePayloadAsParameterSource = null;

	private BeanFactory beanFactory;

	private EvaluationContext evaluationContext;

	/**
	 * Constructor taking an {@link EntityManagerFactory} from which the
	 * {@link EntityManager} can be obtained.
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
	 * See also {@link DefaultJpaOperations} and {@link AbstractJpaOperations}.
	 * @param jpaOperations Must not be null.
	 */
	public JpaExecutor(JpaOperations jpaOperations) {
		Assert.notNull(jpaOperations, "jpaOperations must not be null.");
		this.jpaOperations = jpaOperations;
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	/**
	 * Set the class type which is being used for retrieving entities from the
	 * database.
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
	 * @param nativeQuery The provided SQL query must neither be null nor empty.
	 */
	public void setNativeQuery(String nativeQuery) {

		Assert.isTrue(this.namedQuery == null && this.jpaQuery == null, "You can define only one of the "
				+ "properties 'jpaQuery', 'nativeQuery', 'namedQuery'");
		Assert.hasText(nativeQuery, "nativeQuery must neither be null nor empty.");

		this.nativeQuery = nativeQuery;
	}

	/**
	 * A named query can either refer to a named JPQL based query or a native SQL
	 * query.
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
	 * If set to {@code true} the {@link javax.persistence.EntityManager#flush()} will be called
	 * after persistence operation.
	 * Has the same effect, if the {@link #flushSize} is specified to {@code 1}.
	 * For convenience in cases when the provided entity to persist is not an instance of {@link Iterable}.
	 * @param flush defaults to 'false'.
	 */
	public void setFlush(boolean flush) {
		this.flush = flush;
	}

	/**
	 * If the provided value is greater than {@code 0}, then {@link javax.persistence.EntityManager#flush()}
	 * will be called after persistence operations as well as within batch operations.
	 * This property has precedence over the {@link #flush}, if it is specified to a value greater than {@code 0}.
	 * If the entity to persist is not an instance of {@link Iterable} and this property is greater than {@code 0},
	 * then the entity will be flushed as if the {@link #flush} attribute was set to {@code true}.
	 * @param flushSize defaults to '0'.
	 */
	public void setFlushSize(int flushSize) {
		Assert.state(flushSize >= 0, "'flushSize' cannot be less than '0'.");
		this.flushSize = flushSize;
	}

	/**
	 * If set to {@code true} the {@link javax.persistence.EntityManager#clear()} will be called,
	 * and only if the {@link javax.persistence.EntityManager#flush()} was called after performing persistence
	 * operations.
	 * @param clearOnFlush defaults to 'false'.
	 * @see #setFlush(boolean)
	 * @see #setFlushSize(int)
	 */
	public void setClearOnFlush(boolean clearOnFlush) {
		this.clearOnFlush = clearOnFlush;
	}

	/**
	 * If not set, this property defaults to <code>false</code>, which means that
	 * deletion occurs on a per object basis if a collection of entities is being
	 * deleted.
	 *<p>If set to 'true' the elements of the payload are deleted as a batch
	 * operation. Be aware that this exhibits issues in regards to cascaded deletes.
	 *<p>The specification 'JSR 317: Java Persistence API, Version 2.0' does not
	 * support cascaded deletes in batch operations. The specification states in
	 * chapter 4.10:
	 *<p>"A delete operation only applies to entities of the specified class and
	 * its subclasses. It does not cascade to related entities."
	 * @param deleteInBatch Defaults to 'false' if not set.
	 */
	public void setDeleteInBatch(boolean deleteInBatch) {
		this.deleteInBatch = deleteInBatch;
	}

	/**
	 * If set to 'true', the retrieved objects are deleted from the database upon
	 * being polled. May not work in all situations, e.g. for Native SQL Queries.
	 * @param deleteAfterPoll Defaults to 'false'.
	 */
	public void setDeleteAfterPoll(boolean deleteAfterPoll) {
		this.deleteAfterPoll = deleteAfterPoll;
	}

	/**
	 * @param parameterSourceFactory Must not be null
	 */
	public void setParameterSourceFactory(ParameterSourceFactory parameterSourceFactory) {
		Assert.notNull(parameterSourceFactory, "parameterSourceFactory must not be null.");
		this.parameterSourceFactory = parameterSourceFactory;
	}

	/**
	 * Specify the {@link ParameterSource} that would be used to provide
	 * additional parameters.
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
	 * <p>If the result map contains more than 1 element and
	 * {@link JpaExecutor#expectSingleResult} is <code>true</code>, then a
	 * {@link MessagingException} is thrown.
	 * <p>If set to <code>false</code>, the complete result list is returned as the
	 * payload.
	 * @param expectSingleResult true if a single object is expected.
	 *
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	/**
	 * Set the expression that will be evaluated to get the first result in the query executed.
	 * If a null expression is set, all the results in the result set will be retrieved
	 * @param firstResultExpression The first result expression.
	 * @see javax.persistence.Query#setFirstResult(int)
	 */
	public void setFirstResultExpression(Expression firstResultExpression) {
		this.firstResultExpression = firstResultExpression;
	}

	/**
	 * Set the expression that will be evaluated to get the {@code primaryKey} for
	 * {@link javax.persistence.EntityManager#find(Class, Object)}
	 * @param idExpression the SpEL expression for entity {@code primaryKey}.
	 * @since 4.0
	 */
	public void setIdExpression(Expression idExpression) {
		this.idExpression = idExpression;
	}

	/**
	 * Set the expression for maximum number of results expression. It has be a non null value
	 * Not setting one will default to the behavior of fetching all the records
	 * @param maxResultsExpression The maximum results expression.
	 */
	public void setMaxResultsExpression(Expression maxResultsExpression) {
		Assert.notNull(maxResultsExpression, "maxResultsExpression cannot be null");
		this.maxResultsExpression = maxResultsExpression;
	}

	/**
	 * Set the max number of results to retrieve from the database. Defaults to
	 * 0, which means that all possible objects shall be retrieved.
	 * @param maxNumberOfResults Must not be negative.
	 * @see javax.persistence.Query#setMaxResults(int)
	 */
	public void setMaxNumberOfResults(int maxNumberOfResults) {
		this.setMaxResultsExpression(new ValueExpression<>(maxNumberOfResults));
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Verify and sets the parameters. E.g. initializes the to be used
	 * {@link ParameterSourceFactory}.
	 */
	@Override
	public void afterPropertiesSet() {
		if (!CollectionUtils.isEmpty(this.jpaParameters)) {
			if (this.parameterSourceFactory == null) {
				ExpressionEvaluatingParameterSourceFactory expressionSourceFactory =
						new ExpressionEvaluatingParameterSourceFactory(this.beanFactory);
				expressionSourceFactory.setParameters(this.jpaParameters);
				this.parameterSourceFactory = expressionSourceFactory;

			}
			else {
				throw new IllegalStateException("The 'jpaParameters' and 'parameterSourceFactory' " +
						"are mutually exclusive. Consider to configure parameters on the provided " +
						"'parameterSourceFactory': " + this.parameterSourceFactory);
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

		if (this.flushSize > 0) {
			this.flush = true;
		}
		else if (this.flush) {
			this.flushSize = 1;
		}

		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		}
	}

	/**
	 * Execute the actual Jpa Operation. Call this method, if you need access to
	 * process return values. This methods return a Map that contains either
	 * the number of affected entities or the affected entity itself.
	 *<p>Keep in mind that the number of entities effected by the operation may
	 * not necessarily correlate with the number of rows effected in the database.
	 * @param message The message.
	 * @return Either the number of affected entities when using a JPQL query.
	 * When using a merge/persist the updated/inserted itself is returned.
	 */
	public Object executeOutboundJpaOperation(Message<?> message) {
		ParameterSource paramSource = null;
		if (this.jpaQuery != null || this.nativeQuery != null || this.namedQuery != null) {
			paramSource = determineParameterSource(message);
		}
		if (this.jpaQuery != null) {
			return this.jpaOperations.executeUpdate(this.jpaQuery, paramSource);
		}
		else if (this.nativeQuery != null) {
			return this.jpaOperations.executeUpdateWithNativeQuery(this.nativeQuery, paramSource);
		}
		else if (this.namedQuery != null) {
			return this.jpaOperations.executeUpdateWithNamedQuery(this.namedQuery, paramSource);
		}
		else {
			return executeOutboundJpaOperationOnPersistentMode(message);
		}
	}

	private Object executeOutboundJpaOperationOnPersistentMode(Message<?> message) {
		Object payload = message.getPayload();
		switch (this.persistMode) {
			case PERSIST:
				this.jpaOperations.persist(payload, this.flushSize, this.clearOnFlush);
				return payload;
			case MERGE:
				return this.jpaOperations.merge(payload, this.flushSize, this.clearOnFlush); // NOSONAR
			case DELETE:
				this.jpaOperations.delete(payload);
				if (this.flush) {
					this.jpaOperations.flush();
				}
				return payload;
			default:
				throw new IllegalStateException("Unsupported PersistMode: " + this.persistMode.name());
		}
	}

	/**
	 * Execute the JPA operation. Delegates to {@link JpaExecutor#poll(Message)}.
	 * @return The object or null.
	 */
	@Nullable
	public Object poll() {
		return poll(null);
	}

	/**
	 * Execute a (typically retrieving) JPA operation. The <i>requestMessage</i>
	 * can be used to provide additional query parameters using
	 * {@link JpaExecutor#parameterSourceFactory}. If the
	 * <i>requestMessage</i> parameter is null then
	 * {@link JpaExecutor#parameterSource} is being used for providing query parameters.
	 * @param requestMessage May be null.
	 * @return The payload object, which may be null.
	 */
	@Nullable
	public Object poll(@Nullable final Message<?> requestMessage) {
		final Object payload;

		if (this.idExpression != null) {
			Object id = this.idExpression.getValue(this.evaluationContext, requestMessage); // NOSONAR It can be null
			Assert.state(id != null, "The 'idExpression' cannot evaluate to null.");
			Class<?> entityClazz = this.entityClass;
			if (entityClazz == null && requestMessage != null) {
				entityClazz = requestMessage.getPayload().getClass();
			}
			Assert.state(entityClazz != null, "The entity class to retrieve cannot be null.");
			payload = this.jpaOperations.find(entityClazz, id);
		}
		else {
			final List<?> result;
			int maxNumberOfResults = evaluateExpressionForNumericResult(requestMessage, this.maxResultsExpression);
			if (requestMessage == null) {
				result = doPoll(this.parameterSource, 0, maxNumberOfResults);
			}
			else {
				int firstResult = 0;
				if (this.firstResultExpression != null) {
					firstResult = getFirstResult(requestMessage);
				}
				ParameterSource paramSource = determineParameterSource(requestMessage);
				result = doPoll(paramSource, firstResult, maxNumberOfResults);
			}

			if (result.isEmpty()) {
				payload = null;
			}
			else {
				if (this.expectSingleResult) {
					if (result.size() == 1) {
						payload = result.iterator().next();
					}
					else if (requestMessage != null) {
						throw new MessagingException(requestMessage,
								"The Jpa operation returned more than 1 result for expectSingleResult mode.");
					}
					else {
						throw new MessagingException(
								"The Jpa operation returned more than 1 result for expectSingleResult mode.");
					}
				}
				else {
					payload = result;
				}
			}
		}

		checkDelete(payload);
		return payload;
	}

	private void checkDelete(@Nullable Object payload) {
		if (payload != null && this.deleteAfterPoll) {
			if (payload instanceof Iterable) {
				if (this.deleteInBatch) {
					this.jpaOperations.deleteInBatch((Iterable<?>) payload);
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

			if (this.flush) {
				this.jpaOperations.flush();
			}
		}
	}

	protected List<?> doPoll(ParameterSource jpaQLParameterSource, int firstResult, int maxNumberOfResults) {
		List<?> payload;
		if (this.jpaQuery != null) {
			payload =
					this.jpaOperations.getResultListForQuery(this.jpaQuery, jpaQLParameterSource,
							firstResult, maxNumberOfResults);
		}
		else if (this.nativeQuery != null) {
			payload =
					this.jpaOperations.getResultListForNativeQuery(this.nativeQuery, this.entityClass,
							jpaQLParameterSource, firstResult, maxNumberOfResults);
		}
		else if (this.namedQuery != null) {
			payload =
					this.jpaOperations.getResultListForNamedQuery(this.namedQuery, jpaQLParameterSource,
							firstResult, maxNumberOfResults);
		}
		else if (this.entityClass != null) {
			payload = this.jpaOperations.getResultListForClass(this.entityClass, firstResult, maxNumberOfResults);
		}
		else {
			throw new IllegalStateException("For the polling operation, one of "
					+ "the following properties must be specified: "
					+ "query, namedQuery or entityClass.");
		}
		return payload;
	}

	private int getFirstResult(final Message<?> requestMessage) {
		return evaluateExpressionForNumericResult(requestMessage, this.firstResultExpression);
	}

	private int evaluateExpressionForNumericResult(@Nullable final Message<?> requestMessage,
			@Nullable Expression expression) {

		int evaluatedResult = 0;
		if (expression != null) {
			Object evaluationResult = expression.getValue(this.evaluationContext, requestMessage); // NOSONAR can be
			// null
			if (evaluationResult != null) {
				if (evaluationResult instanceof Number) {
					evaluatedResult = ((Number) evaluationResult).intValue();
				}
				else if (evaluationResult instanceof String) {
					try {
						evaluatedResult = Integer.parseInt((String) evaluationResult);
					}
					catch (NumberFormatException e) {
						throw new IllegalArgumentException(
								"Value " + evaluationResult + " passed as cannot be " +
										"parsed to a number, expected to be numeric", e);
					}
				}
				else {
					throw new IllegalArgumentException("Expected the value to be a Number got "
							+ evaluationResult.getClass().getName());
				}
			}
		}
		return evaluatedResult;
	}

	private ParameterSource determineParameterSource(final Message<?> requestMessage) {
		if (this.usePayloadAsParameterSource) {
			return this.parameterSourceFactory.createParameterSource(requestMessage.getPayload());
		}
		else {
			return this.parameterSourceFactory.createParameterSource(requestMessage);
		}
	}

}
