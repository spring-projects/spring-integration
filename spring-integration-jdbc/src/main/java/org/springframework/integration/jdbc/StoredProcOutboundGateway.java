/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

import java.util.Map;

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An {@link AbstractReplyProducingMessageHandler} implementation for performing
 * RDBMS stored procedures which return results.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class StoredProcOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final StoredProcExecutor executor;

	private volatile boolean expectSingleResult = false;

	private boolean requiresReplyExplicitlySet;

	/**
	 * Constructor taking {@link StoredProcExecutor}.
	 * @param storedProcExecutor Must not be null.
	 */
	public StoredProcOutboundGateway(StoredProcExecutor storedProcExecutor) {
		Assert.notNull(storedProcExecutor, "storedProcExecutor must not be null.");
		this.executor = storedProcExecutor;
	}

	@Override
	public void setRequiresReply(boolean requiresReply) {
		super.setRequiresReply(requiresReply);
		this.requiresReplyExplicitlySet = true;
	}

	/**
	 * This parameter indicates that only one result object shall be returned from
	 * the Stored Procedure/Function Call. If set to {@code true}, a {@code resultMap} that contains
	 * only 1 element, will have that 1 element extracted and returned as payload.
	 * <p> If the {@code resultMap} contains more than 1 element and {@code expectSingleResult == true},
	 * then a {@link org.springframework.messaging.MessagingException} is thrown.
	 * <p> Otherwise the complete {@code resultMap} is returned as the {@link Message} payload.
	 * <p> Important Note: Several databases such as H2 are not fully supported.
	 * The H2 database, for example, does not fully support the
	 * {@link java.sql.CallableStatement}
	 * semantics and when executing function calls against H2, a result list is
	 * returned rather than a single value.
	 * <p> Therefore, even if you set {@code expectSingleResult = true}, you may end up with
	 * a collection being returned.
	 * <p> When set to {@code true}, a {@link #setRequiresReply(boolean)} is called
	 * with {@code true} as well, indicating that exactly single result is expected
	 * and {@code null} isn't appropriate value.
	 * A {@link org.springframework.integration.handler.ReplyRequiredException} is thrown
	 * in case of {@code null} result.
	 * @param expectSingleResult true if a single result is expected.
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}

	@Override
	public String getComponentType() {
		return "jdbc:stored-proc-outbound-gateway";
	}

	@Override
	protected void doInit() {
		super.doInit();
		if (!this.requiresReplyExplicitlySet) {
			setRequiresReply(this.expectSingleResult);
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Map<String, Object> resultMap = this.executor.executeStoredProcedure(requestMessage);

		final Object payload;

		if (resultMap.isEmpty()) {
			return null;
		}
		else {
			if (this.expectSingleResult && resultMap.size() == 1) {
				payload = resultMap.values().iterator().next();
			}
			else if (this.expectSingleResult && resultMap.size() > 1) {
				throw new IllegalStateException("Stored Procedure/Function call returned more than "
						+ "1 result object and expectSingleResult was 'true'.");
			}
			else {
				payload = resultMap;
			}
		}

		return payload;
	}

}
