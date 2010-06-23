/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.http;

import org.springframework.http.MediaType;

/**
 * Strategy for resolving the content type of a given object. The content type
 * will be represented as an instance of the {@link MediaType} enum.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public interface ContentTypeResolver {

	/**
	 * Resolves the content type of a given object.
	 * 
	 * @param content the object whose content type should be resolved
	 */
	MediaType resolveContentType(Object content);

	/**
	 * Resolves the content type of a given String instance and charset name.
	 * 
	 * @param content the String whose content type should be resolved
	 * @param charset charset name
	 */
	MediaType resolveContentType(String content, String charset);

}
