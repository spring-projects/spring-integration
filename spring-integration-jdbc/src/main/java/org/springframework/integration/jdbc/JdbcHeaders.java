/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.jdbc;

import org.springframework.integration.MessageHeaders;

/**
 * Pre-defined names and prefixes to be used for setting JDBC
 * specific attributes via Spring Integration {@link MessageHeaders}.
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public abstract class JdbcHeaders {

	/**
	 * Prefix used for JMS API related headers in order to distinguish from
	 * user-defined headers and other internal headers (e.g. correlationId).
	 */
	public static final String PREFIX = "jdbc_";

	/** Identifies the name of the Store Procedure or Stored Function. */
	public static final String STORED_PROCEDURE_NAME = PREFIX + "storedProcedureName";

}
