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

package org.springframework.commons.serializer;

import java.io.IOException;
import java.io.InputStream;

/**
 * An standard interface to convert from data in an InputStream to 
 * an Object.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public interface InputStreamingConverter<T> {
	
	/**
	 * Read (assemble an object of type T) from an InputStream.
	 * @param inputStream The InputStream.
	 * @return The object.
	 * @throws IOException
	 */
	public T convert(InputStream inputStream) throws IOException;
	
}
