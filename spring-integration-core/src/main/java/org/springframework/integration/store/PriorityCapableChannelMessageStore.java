/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.store;

/**
 * A {@link ChannelMessageStore} that supports the
 * notion of message priority. It is left to implementations to determine what
 * that means and whether all or a subset of priorities are supported.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface PriorityCapableChannelMessageStore extends ChannelMessageStore {

	/**
	 * @return true if message priority is enabled in this channel message store.
	 */
	boolean isPriorityEnabled();

}
