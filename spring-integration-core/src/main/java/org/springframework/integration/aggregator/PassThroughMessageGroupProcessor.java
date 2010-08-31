/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingOperations;
import org.springframework.integration.store.MessageGroup;

/**
 * This implementation of MessageGroupProcessor will forward all messages inside the group to the given output channel.
 * This is useful if there is no requirement to process the messages, but they should just be blocked as a group until
 * their ReleaseStrategy lets them pass through.
 *
 * @author Iwein Fuld
 * @since 2.0.0
 */
public class PassThroughMessageGroupProcessor implements MessageGroupProcessor {

    public void processAndSend(MessageGroup group, MessagingOperations messagingTemplate, MessageChannel outputChannel) {
        for (Message<?> message : group.getUnmarked()) {
            messagingTemplate.send(outputChannel, message);
        }
    }

}
