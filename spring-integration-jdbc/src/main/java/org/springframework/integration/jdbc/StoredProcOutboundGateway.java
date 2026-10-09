/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;

/**
 * An {@link AbstractReplyProducingMessageHandler} implementation for performing
 * RDBMS stored procedures which return results.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jdbc.outbound.StoredProcOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class StoredProcOutboundGateway extends org.springframework.integration.jdbc.outbound.StoredProcOutboundGateway {

	/**
	 * Constructor taking {@link StoredProcExecutor}.
	 * @param storedProcExecutor Must not be null.
	 */
	public StoredProcOutboundGateway(StoredProcExecutor storedProcExecutor) {
		super(storedProcExecutor);
	}

}
