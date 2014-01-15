/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Collaborator for JDBC adapters which allows creation of
 * instances of {@link SqlParameterSource} for use in update operations.
 *
 * @author Jonas Partner
 * @since 2.0
 */
public interface SqlParameterSourceFactory {

	/**
	 * Return a new {@link SqlParameterSource}.
	 *
	 * @param input the raw message or query result to be transformed into a SqlParameterSource
	 * @return The parameter source.
	 */
	public SqlParameterSource createParameterSource(Object input);

}
