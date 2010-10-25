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
package org.springframework.integration.twitter;

import org.springframework.integration.twitter.core.DirectMessage;
import org.springframework.integration.twitter.core.Status;
import org.springframework.stereotype.Component;


@Component
public class TwitterAnnouncer {
	public void dm(DirectMessage directMessage) {
		System.out.println("A direct message has been received from " +
				directMessage.getSender().getScreenName() + " with text " + directMessage.getText());
	}

	public void mention(Status s) {
		System.out.println("A tweet mentioning (or replying) to " + "you was received having text " + s.getText() + " from " + s.getSource());
	}

	public void updates(Status t) {
		System.out.println("Received timeline update: " + t.getText() + " from " + t.getSource());
	}
}
