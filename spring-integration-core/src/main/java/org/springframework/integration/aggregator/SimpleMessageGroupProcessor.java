/*
 * Copyright 2015-2019 the original author or authors.
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

package org.springframework.integration.aggregator;

import org.springframework.integration.store.MessageGroup;

/**
 * A {@link MessageGroupProcessor} that simply returns the messages in the group.
 * It can be used to configure an aggregator as a barrier, such that when the group
 * is complete, the grouped messages are released as individual messages.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class SimpleMessageGroupProcessor implements MessageGroupProcessor {

	@Override
	public Object processMessageGroup(MessageGroup group) {
		return group.getMessages();
	}

}
