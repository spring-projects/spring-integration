/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.integration.r2dbc.outbound;

import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.relational.core.query.Query;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReactiveMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link org.springframework.messaging.ReactiveMessageHandler} which writes
 * Message payload into a Relational Database, using reactive r2dbc support.
 *
 * @author Rohan Mukesh
 *
 * @since 5.3
 */
public class ReactiveR2dbcMessageHandler extends AbstractReactiveMessageHandler {

	private DatabaseClient databaseClient;

	private R2dbcEntityOperations r2dbcEntityOperations;

	private ReactiveDataAccessStrategy reactiveDataAccessStrategy;

	private Type mode;

	private Class<?> entityClass;

	private volatile boolean initialized = false;

	private StandardEvaluationContext evaluationContext;

	/**
	 * Prepared statement to use in association with high throughput ingestion.
	 */
	private String query;


	/**
	 * Construct this instance using a fully created and initialized instance of provided
	 * {@link R2dbcEntityOperations}
	 * @param r2dbcEntityOperations The R2dbcEntityOperations implementation.
	 */

	public ReactiveR2dbcMessageHandler(R2dbcEntityOperations r2dbcEntityOperations) {
		this(r2dbcEntityOperations, Type.INSERT);
	}

	public ReactiveR2dbcMessageHandler(R2dbcEntityOperations r2dbcEntityOperations,
									   ReactiveR2dbcMessageHandler.Type queryType) {
		Assert.notNull(r2dbcEntityOperations, "'r2dbc' must not be null");
		this.r2dbcEntityOperations = r2dbcEntityOperations;
		this.mode = queryType;
	}

	public ReactiveR2dbcMessageHandler(DatabaseClient databaseClient, ReactiveDataAccessStrategy reactiveDataAccessStrategy,
									   ReactiveR2dbcMessageHandler.Type queryType) {
		Assert.notNull(databaseClient, "'databaseClient' must not be null");
		this.databaseClient = databaseClient;
		this.reactiveDataAccessStrategy = reactiveDataAccessStrategy;
		this.mode = queryType;
	}

	/**
	 * Allow you to set the type of the entityClass that will be passed to the
	 * {@link R2dbcEntityOperations#delete(Query, Class)}
	 * method.
	 * @param entityClass The entity class.
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "'entityClass' must not be null");
		this.entityClass = entityClass;
	}

	public void setQuery(String query) {
		Assert.hasText(query, "'query' must not be empty");
		this.query = query;
	}

	@Override
	public String getComponentType() {
		return "r2dbc:reactive-outbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		if (this.r2dbcEntityOperations == null) {
			R2dbcEntityTemplate r2dbcEntityOperations = new R2dbcEntityTemplate(databaseClient, this.reactiveDataAccessStrategy);
			r2dbcEntityOperations.setBeanFactory(getApplicationContext().getParentBeanFactory());
			this.r2dbcEntityOperations = r2dbcEntityOperations;
		}
		this.initialized = true;
	}

	@Override
	protected Mono<Void> handleMessageInternal(Message<?> message) {
		Assert.isTrue(this.initialized, "This class is not yet initialized. Invoke its afterPropertiesSet() method");
		Object payload = message.getPayload();
		Type mode = this.mode;
		switch (mode) {
			case INSERT:
				return handleInsert(payload);
			case UPDATE:
				return handleUpdate(payload);
			case DELETE:
				return handleDelete(payload);
		}
		return null;
	}


	private Mono<Void> handleDelete(Object payload) {
		return this.r2dbcEntityOperations.delete(payload).then();
	}

	private Mono<Void> handleUpdate(Object payload) {
		return this.r2dbcEntityOperations.update(payload).then();
	}

	private Mono<Void> handleInsert(Object payload) {
		return this.r2dbcEntityOperations.insert(payload).then();
	}

	/**
	/**
	 * The mode for the {@link ReactiveR2dbcMessageHandler}.
	 */
	public enum Type {

		/**
		 * Set a {@link ReactiveR2dbcMessageHandler} into an {@code insert} mode.
		 */
		INSERT,

		/**
		 * Set a {@link ReactiveR2dbcMessageHandler} into an {@code update} mode.
		 */
		UPDATE,

		/**
		 * Set a {@link ReactiveR2dbcMessageHandler} into a {@code delete} mode.
		 */
		DELETE,

	}

}
