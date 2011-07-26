/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.mongodb.store;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import com.mongodb.Mongo;

/**
 * @author Mark Fisher
 */
public class MongoMessageStoreTests {

	@Test @Ignore
	public void addAndRemove() throws Exception {
		Mongo mongo = new Mongo();
		MongoMessageStore store = new MongoMessageStore(mongo, "test");
		Message<?> message = MessageBuilder.withPayload("UUID again and again").build();
		Message<?> claimCheck = store.addMessage(message);
		System.out.println("added: " + claimCheck);
		//Thread.sleep(10000);
		message = store.removeMessage(message.getHeaders().getId());
		System.out.println("removed: " + message);
	}

}
