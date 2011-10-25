/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import org.springframework.integration.store.MessageGroup;

/**
 * This implementation of MessageGroupProcessor will return all messages inside the group.
 * This is useful if there is no requirement to process the messages, but they should just be
 * blocked as a group until their ReleaseStrategy lets them pass through.
 * 
 * @author Iwein Fuld
 * @since 2.0.0
 */
public class PassThroughMessageGroupProcessor implements MessageGroupProcessor {

	public Object processMessageGroup(MessageGroup group) {
		return group.getMessages();
	}

}
