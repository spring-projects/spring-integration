/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

/**
 * A polling channel adapter that creates messages from the payload returned by
 * executing a stored procedure or Sql function. Optionally an update can be executed
 * after the execution of the Stored Procedure or Function in order to update
 * processed rows.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jdbc.inbound.StoredProcPollingChannelAdapter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class StoredProcPollingChannelAdapter
		extends org.springframework.integration.jdbc.inbound.StoredProcPollingChannelAdapter {

	/**
	 * Constructor taking {@link StoredProcExecutor}.
	 * @param storedProcExecutor Must not be null.
	 */
	public StoredProcPollingChannelAdapter(StoredProcExecutor storedProcExecutor) {
		super(storedProcExecutor);
	}

}
