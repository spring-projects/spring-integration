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

import org.junit.Test;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.Message;
import org.springframework.integration.mongodb.rules.MongodbAvailable;
import org.springframework.integration.mongodb.rules.MongodbAvailableTests;
import org.springframework.integration.support.MessageBuilder;

import com.mongodb.Mongo;

import static org.junit.Assert.assertNotNull;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MongoMessageStoreTests extends MongodbAvailableTests{

	@Test 
	@MongodbAvailable
	public void addGetWithStringPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoMessageStore store = new MongoMessageStore(mongoDbFactory);
		Message<?> messageToStore = MessageBuilder.withPayload("Hello").build();
		//System.out.println("before: " + messageToStore);
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		//System.out.println("after: " + retrievedMessage);
		assertNotNull(retrievedMessage);
	}
	
	
	@Test 
	@MongodbAvailable
	public void addGetWithObjectDefaultConstructorPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoMessageStore store = new MongoMessageStore(mongoDbFactory);
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = MessageBuilder.withPayload(p).build();
		//System.out.println("before: " + messageToStore);
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		//System.out.println("after: " + retrievedMessage);
		assertNotNull(retrievedMessage);
	}


	public static class Person {

		private String fname;

		private String lname;

		public String getFname() {
			return fname;
		}

		public void setFname(String fname) {
			this.fname = fname;
		}

		public String getLname() {
			return lname;
		}

		public void setLname(String lname) {
			this.lname = lname;
		}
	}

}
