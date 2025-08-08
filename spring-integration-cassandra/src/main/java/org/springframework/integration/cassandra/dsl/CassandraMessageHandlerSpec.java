/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.datastax.oss.driver.api.core.cql.Statement;

import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.data.cassandra.core.cql.WriteOptions;
import org.springframework.expression.Expression;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.messaging.Message;

/**
 * The {@link MessageHandlerSpec} for {@link CassandraMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class CassandraMessageHandlerSpec extends
		MessageHandlerSpec<CassandraMessageHandlerSpec, CassandraMessageHandler> {

	private final Map<String, Expression> parameterExpressions = new HashMap<>();

	protected CassandraMessageHandlerSpec(ReactiveCassandraOperations cassandraOperations) {
		this.target = new CassandraMessageHandler(cassandraOperations);
	}

	protected CassandraMessageHandlerSpec(ReactiveCassandraOperations cassandraOperations,
			CassandraMessageHandler.Type queryType) {

		this.target = new CassandraMessageHandler(cassandraOperations, queryType);
	}

	protected CassandraMessageHandlerSpec producesReply(boolean producesReply) {
		this.target.setProducesReply(producesReply);
		return _this();
	}

	/**
	 * Set an ingest query.
	 * @param ingestQuery ingest query to use.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec ingestQuery(String ingestQuery) {
		this.target.setIngestQuery(ingestQuery);
		return _this();
	}

	/**
	 * Set a {@link WriteOptions} for {@code INSERT}, {@code UPDATE} or {@code DELETE} operations.
	 * @param writeOptions the {@link WriteOptions} to use.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec writeOptions(WriteOptions writeOptions) {
		this.target.setWriteOptions(writeOptions);
		return _this();
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Statement} against request message.
	 * @param statementExpression the SpEL expression to use.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec statementExpression(String statementExpression) {
		return statementExpression(PARSER.parseExpression(statementExpression));
	}

	/**
	 * Set a SpEL expression to evaluate a {@link Statement} against request message.
	 * @param statementExpression the SpEL expression to use.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec statementExpression(Expression statementExpression) {
		this.target.setStatementExpression(statementExpression);
		return _this();
	}

	/**
	 *  Set a {@link Function} to evaluate a {@link Statement} against request message.
	 * @param statementFunction the function to use.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec statementFunction(Function<Message<?>, Statement<?>> statementFunction) {
		this.target.setStatementProcessor(statementFunction::apply);
		return _this();
	}

	/**
	 * Set a {@code SELECT} query.
	 * @param query the CQL query to execute
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec query(String query) {
		this.target.setQuery(query);
		return _this();
	}

	/**
	 * Set a map for named parameters and expressions for their values against a request message.
	 * @param parameterExpressions the map to use.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec parameterExpressions(Map<String, Expression> parameterExpressions) {
		this.target.setParameterExpressions(parameterExpressions);
		return _this();
	}

	/**
	 * Add a named bindable parameter with a SpEL expression to evaluate its value against a request message.
	 * @param name the name of parameter.
	 * @param expression the SpEL expression for parameter value.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec parameter(String name, String expression) {
		return parameter(name, PARSER.parseExpression(expression));
	}

	/**
	 * Add a named bindable parameter with a function to evaluate its value against a request message.
	 * @param name the name of parameter.
	 * @param function the function for parameter value.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec parameter(String name, Function<Message<?>, ?> function) {
		return parameter(name, new FunctionExpression<>(function));
	}

	/**
	 * Add a named bindable parameter with a SpEL expression to evaluate its value against a request message.
	 * @param name the name of parameter.
	 * @param expression the SpEL expression for parameter value.
	 * @return this spec
	 */
	public CassandraMessageHandlerSpec parameter(String name, Expression expression) {
		this.parameterExpressions.put(name, expression);
		return parameterExpressions(this.parameterExpressions);
	}

}
