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

import org.springframework.expression.Expression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.OutboundGatewayType;

/**
 * A {@link JpaBaseOutboundEndpointSpec} extension for the
 * {@link org.springframework.integration.jpa.outbound.JpaOutboundGateway} with
 * {@link org.springframework.integration.jpa.support.OutboundGatewayType#RETRIEVING} mode.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class JpaRetrievingOutboundGatewaySpec extends JpaBaseOutboundEndpointSpec<JpaRetrievingOutboundGatewaySpec> {

	protected JpaRetrievingOutboundGatewaySpec(JpaExecutor jpaExecutor) {
		super(jpaExecutor);
		this.target.setGatewayType(OutboundGatewayType.RETRIEVING);
		this.target.setRequiresReply(true);
	}

	/**
	 * This parameter indicates that only one result object shall be returned as
	 * a result from the executed JPA operation. If set to <code>true</code> and
	 * the result list from the JPA operations contains only 1 element, then that
	 * 1 element is extracted and returned as payload.
	 * @param expectSingleResult true if a single object is expected.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec expectSingleResult(boolean expectSingleResult) {
		this.jpaExecutor.setExpectSingleResult(expectSingleResult);
		return this;
	}

	/**
	 * Specify a first result in the query executed.
	 * @param firstResult the first result to use.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec firstResult(int firstResult) {
		return firstResultExpression(new ValueExpression<>(firstResult));
	}

	/**
	 * Specify a SpEL expression that will be evaluated to get the first result in the query executed.
	 * @param firstResultExpression The first result expression.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec firstResultExpression(String firstResultExpression) {
		return firstResultExpression(PARSER.parseExpression(firstResultExpression));
	}

	/**
	 * Specify a SpEL expression that will be evaluated to get the first result in the query executed.
	 * @param firstResultExpression The first result expression.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec firstResultExpression(Expression firstResultExpression) {
		this.jpaExecutor.setFirstResultExpression(firstResultExpression);
		return this;
	}

	/**
	 * Specify a SpEL expression that will be evaluated to get the {@code primaryKey} for
	 * {@link jakarta.persistence.EntityManager#find(Class, Object)}.
	 * @param idExpression the SpEL expression for entity {@code primaryKey}.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec idExpression(String idExpression) {
		return idExpression(PARSER.parseExpression(idExpression));
	}

	/**
	 * Specify a SpEL expression that will be evaluated to get the {@code primaryKey} for
	 * {@link jakarta.persistence.EntityManager#find(Class, Object)}.
	 * @param idExpression the SpEL expression for entity {@code primaryKey}.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec idExpression(Expression idExpression) {
		this.jpaExecutor.setIdExpression(idExpression);
		return this;
	}

	/**
	 * Set the maximum number of results expression. It has be a non null value
	 * Not setting one will default to the behavior of fetching all the records
	 * @param maxResults the maximum number of results to retrieve
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec maxResults(int maxResults) {
		return maxResultsExpression(new ValueExpression<>(maxResults));
	}

	/**
	 * Specify a SpEL expression for maximum number of results expression.
	 * Not setting one will default to the behavior of fetching all the records
	 * @param maxResultsExpression The maximum results expression.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec maxResultsExpression(String maxResultsExpression) {
		return maxResultsExpression(PARSER.parseExpression(maxResultsExpression));
	}

	/**
	 * Specify a SpEL expression for maximum number of results expression.
	 * Not setting one will default to the behavior of fetching all the records
	 * @param maxResultsExpression The maximum results expression.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec maxResultsExpression(Expression maxResultsExpression) {
		this.jpaExecutor.setMaxResultsExpression(maxResultsExpression);
		return this;
	}

	/**
	 * If set to {@code true}, the retrieved objects are deleted from the database upon
	 * being polled. May not work in all situations, e.g. for Native SQL Queries.
	 * @param deleteAfterPoll defaults to {@code false}.
	 * @return the spec
	 */
	public JpaRetrievingOutboundGatewaySpec deleteAfterPoll(boolean deleteAfterPoll) {
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
	public JpaRetrievingOutboundGatewaySpec deleteInBatch(boolean deleteInBatch) {
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
	public JpaRetrievingOutboundGatewaySpec flushAfterDelete(boolean flush) {
		this.jpaExecutor.setFlush(flush);
		return this;
	}

}
