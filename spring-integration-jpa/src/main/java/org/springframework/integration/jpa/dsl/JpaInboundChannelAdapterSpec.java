/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.jpa.dsl;

import java.util.Collections;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.inbound.JpaPollingChannelAdapter;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;

/**
 * A {@link MessageSourceSpec} for a {@link JpaPollingChannelAdapter}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class JpaInboundChannelAdapterSpec
		extends MessageSourceSpec<JpaInboundChannelAdapterSpec, JpaPollingChannelAdapter>
		implements ComponentsRegistration {

	protected final JpaExecutor jpaExecutor; // NOSONAR - final

	protected JpaInboundChannelAdapterSpec(JpaExecutor jpaExecutor) {
		this.jpaExecutor = jpaExecutor;
		this.target = new JpaPollingChannelAdapter(this.jpaExecutor);
	}

	/**
	 * Specify the class type which is being used for retrieving entities from the database.
	 * @param entityClass the entity {@link Class} to use
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec entityClass(Class<?> entityClass) {
		this.jpaExecutor.setEntityClass(entityClass);
		return this;
	}

	/**
	 * Specify a JPA query to perform persistent operation.
	 * @param jpaQuery the JPA query to use.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec jpaQuery(String jpaQuery) {
		this.jpaExecutor.setJpaQuery(jpaQuery);
		return this;
	}

	/**
	 * Specify a native SQL query to perform persistent operation.
	 * @param nativeQuery the native SQL query to use.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec nativeQuery(String nativeQuery) {
		this.jpaExecutor.setNativeQuery(nativeQuery);
		return this;
	}

	/**
	 * Specify a name a named JPQL based query or a native SQL query.
	 * @param namedQuery the name of the pre-configured query.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec namedQuery(String namedQuery) {
		this.jpaExecutor.setNamedQuery(namedQuery);
		return this;
	}

	/**
	 * If set to 'true', the retrieved objects are deleted from the database upon
	 * being polled. May not work in all situations, e.g. for Native SQL Queries.
	 * @param deleteAfterPoll Defaults to 'false'.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec deleteAfterPoll(boolean deleteAfterPoll) {
		this.jpaExecutor.setDeleteAfterPoll(deleteAfterPoll);
		return this;
	}

	/**
	 * If not set, this property defaults to <code>false</code>, which means that
	 * deletion occurs on a per-object basis if a collection of entities is being
	 * deleted.
	 *<p>If set to 'true' the elements of the payload are deleted as a batch
	 * operation. Be aware that this exhibits issues in regard to cascaded deletes.
	 *<p>The specification 'JSR 317: Java Persistence API, Version 2.0' does not
	 * support cascaded deletes in batch operations. The specification states in
	 * chapter 4.10:
	 *<p>"A delete operation only applies to entities of the specified class and
	 * its subclasses. It does not cascade to related entities."
	 * @param deleteInBatch Defaults to 'false' if not set.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec deleteInBatch(boolean deleteInBatch) {
		this.jpaExecutor.setDeleteInBatch(deleteInBatch);
		return this;
	}

	/**
	 * If set to {@code true} the {@link jakarta.persistence.EntityManager#flush()} will be called
	 * after persistence operation.
	 * Has the same effect, if the {@code flushSize} is specified to {@code 1}.
	 * For convenience in cases when the provided entity to persist is not an instance of {@link Iterable}.
	 * @param flush defaults to 'false'.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec flushAfterDelete(boolean flush) {
		this.jpaExecutor.setFlush(flush);
		return this;
	}

	/**
	 * Specify a {@link ParameterSource} that would be used to provide additional parameters.
	 * @param parameterSource the {@link ParameterSource} to use.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec parameterSource(ParameterSource parameterSource) {
		this.jpaExecutor.setParameterSource(parameterSource);
		return this;
	}

	/**
	 * This parameter indicates that only one result object shall be returned as
	 * a result from the executed JPA operation. If set to <code>true</code> and
	 * the result list from the JPA operations contains only 1 element, then that
	 * 1 element is extracted and returned as payload.
	 * @param expectSingleResult true if a single object is expected.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec expectSingleResult(boolean expectSingleResult) {
		this.jpaExecutor.setExpectSingleResult(expectSingleResult);
		return this;
	}

	/**
	 * Set the maximum number of results expression. It has to be a non-null value
	 * Not setting one will default to the behavior of fetching all the records
	 * @param maxResults the maximum number of results to retrieve
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec maxResults(int maxResults) {
		return maxResultsExpression(new ValueExpression<>(maxResults));
	}

	/**
	 * Specify a SpEL expression for maximum number of results expression.
	 * Not setting one will default to the behavior of fetching all the records
	 * @param maxResultsExpression The maximum results expression.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec maxResultsExpression(String maxResultsExpression) {
		return maxResultsExpression(PARSER.parseExpression(maxResultsExpression));
	}

	/**
	 * Specify a SpEL expression for maximum number of results expression.
	 * Not setting one will default to the behavior of fetching all the records
	 * @param maxResultsExpression The maximum results expression.
	 * @return the spec
	 */
	public JpaInboundChannelAdapterSpec maxResultsExpression(Expression maxResultsExpression) {
		this.jpaExecutor.setMaxResultsExpression(maxResultsExpression);
		return this;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(this.jpaExecutor, null);
	}

}
