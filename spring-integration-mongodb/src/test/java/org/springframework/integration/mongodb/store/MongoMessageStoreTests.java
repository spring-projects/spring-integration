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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import com.mongodb.Mongo;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class MongoMessageStoreTests {

	@Test @Ignore
	public void addGetWithStringPayload() throws Exception {
		Mongo mongo = new Mongo();
		MongoMessageStore store = new MongoMessageStore(mongo, "test");
		Message<?> message = MessageBuilder.withPayload("Hello").build();
		System.out.println(message);
		store.addMessage(message);
		assertNotNull(store.getMessage(message.getHeaders().getId()));
	}
	
	
	@Test @Ignore
	public void addGetWithObjectDefaultConstructorPayload() throws Exception {
		Mongo mongo = new Mongo();
		MongoMessageStore store = new MongoMessageStore(mongo, "test");
		Person p = new Person();
		p.setFname("John");
		p.setLname("Doe");
		
		Message<?> message = MessageBuilder.withPayload(p).build();
		System.out.println(message);
		store.addMessage(message);
		Message<?> m =  store.getMessage(message.getHeaders().getId());
		assertNotNull(m);
		//assertEquals("John", m.getPayload().getFname());
	}
	
	public static class Person{
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
