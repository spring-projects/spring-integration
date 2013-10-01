/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.integration.http.support;

/**
 * Utility class for accessing HTTP integration components from the {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public final class HttpContextUtils {

	/**
	 * @see org.springframework.integration.http.config.HttpInboundEndpointParser
	 */
	public static final String HANDLER_MAPPING_BEAN_NAME = "integrationRequestMappingHandlerMapping";

}
