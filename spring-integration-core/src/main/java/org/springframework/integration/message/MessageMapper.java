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

package org.springframework.integration.message;

/**
 * Strategy interface for mapping between messages and objects.
 * 
 * @author Mark Fisher
 */
public interface MessageMapper<M,O> {

	/**
	 * Map to a {@link Message} from the given object.
	 */
	Message<M> toMessage(O source);

	/**
	 * Map from the given {@link Message} to an object.
	 */
	O fromMessage(Message<M> message);

}
