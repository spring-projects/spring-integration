/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.util;

import java.io.Serializable;

/**
 * A strategy for generating ids to uniquely identify integration artifacts such
 * as Messages.
 * 
 * @author Keith Donald
 */
public interface UidGenerator {

	/**
	 * Generate a new unique id.
	 * @return a serializable id, guaranteed to be unique in some context
	 */
	public Serializable generateUid();

	/**
	 * Convert the string-encoded uid into its original object form.
	 * @param encodedUid the string encoded uid
	 * @return the converted uid
	 */
	public Serializable parseUid(String encodedUid);

}
