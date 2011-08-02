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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.Message;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;

import com.mongodb.Mongo;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MongoMessageStoreTests extends MongoDbAvailableTests{

	@Test 
	@MongoDbAvailable
	public void addGetWithStringPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoMessageStore store = new MongoMessageStore(mongoDbFactory);
		Message<?> messageToStore = MessageBuilder.withPayload("Hello").build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(messageToStore, retrievedMessage);
	}

	@Test 
	@MongoDbAvailable
	public void addGetWithObjectDefaultConstructorPayload() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		MongoMessageStore store = new MongoMessageStore(mongoDbFactory);
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		Message<?> messageToStore = MessageBuilder.withPayload(p).build();
		store.addMessage(messageToStore);
		Message<?> retrievedMessage = store.getMessage(messageToStore.getHeaders().getId());
		assertNotNull(retrievedMessage);
		assertEquals(messageToStore.getPayload(), retrievedMessage.getPayload());
		assertEquals(messageToStore.getHeaders(), retrievedMessage.getHeaders());
		assertEquals(messageToStore, retrievedMessage);
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((fname == null) ? 0 : fname.hashCode());
			result = prime * result + ((lname == null) ? 0 : lname.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Person other = (Person) obj;
			if (fname == null) {
				if (other.fname != null) {
					return false;
				}
			}
			else if (!fname.equals(other.fname)) {
				return false;
			}
			if (lname == null) {
				if (other.lname != null) {
					return false;
				}
			}
			else if (!lname.equals(other.lname)) {
				return false;
			}
			return true;
		}
	}

}
