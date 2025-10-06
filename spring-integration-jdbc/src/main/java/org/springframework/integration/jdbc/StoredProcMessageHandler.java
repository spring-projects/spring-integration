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

package org.springframework.integration.jdbc;

/**
 * A message handler that executes Stored Procedures for update purposes.
 * <p>
 * Stored procedure parameter values are by default automatically extracted from
 * the Payload if the payload's bean properties match the parameters of the Stored
 * Procedure.
 * <p>
 * This may be sufficient for basic use cases. For more sophisticated options
 * consider passing in one or more
 * {@link org.springframework.integration.jdbc.storedproc.ProcedureParameter}.
 * <p>
 * If you need to handle the return parameters of the called stored procedure
 * explicitly, please consider using a {@link org.springframework.integration.jdbc.outbound.StoredProcOutboundGateway} instead.
 * <p>
 * Also, if you need to execute SQL Functions, please also use the
 * {@link org.springframework.integration.jdbc.outbound.StoredProcOutboundGateway}.
 * As functions are typically used to look up values, only,
 * the Stored Procedure message handler purposefully does not support SQL function calls.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jdbc.outbound.StoredProcMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class StoredProcMessageHandler extends org.springframework.integration.jdbc.outbound.StoredProcMessageHandler {

	/**
	 * Constructor passing in the {@link StoredProcExecutor}.
	 * @param storedProcExecutor Must not be null.
	 */
	public StoredProcMessageHandler(StoredProcExecutor storedProcExecutor) {
		super(storedProcExecutor);
	}

}
