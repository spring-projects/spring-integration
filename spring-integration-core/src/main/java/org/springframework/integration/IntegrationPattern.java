/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration;

/**
 * Indicates that a component implements some Enterprise Integration Pattern.
 *
 * @author Artem Bilan
 *
 * @since 5.2.1
 *
 * @see IntegrationPatternType
 * @see <a href="https://www.enterpriseintegrationpatterns.com/patterns/messaging">EIP official site</a>
 */
public interface IntegrationPattern {

	/**
	 * Return a pattern type this component implements.
	 * @return the {@link IntegrationPatternType} this component implements.
	 */
	IntegrationPatternType getIntegrationPatternType();

}
