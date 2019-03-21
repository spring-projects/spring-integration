/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.jpa.support.parametersource;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public interface ParameterSource {

	/**
	 * Determine whether there is a value for the specified named parameter.
	 * @param paramName the name of the parameter
	 * @return whether there is a value defined
	 */
	boolean hasValue(String paramName);

	/**
	 * Return the parameter value for the requested named parameter.
	 * @param paramName the name of the parameter
	 * @return the value of the specified parameter
	 */
	Object getValue(String paramName);

}
