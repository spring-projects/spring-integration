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
