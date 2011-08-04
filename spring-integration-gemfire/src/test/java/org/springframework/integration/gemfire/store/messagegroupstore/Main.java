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

package org.springframework.integration.gemfire.store.messagegroupstore;

import java.util.Arrays;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Simple example demonstrating the use of a {@link org.springframework.integration.gemfire.store.KeyValueMessageGroupStore}.
 *
 * @author Josh Long
 * @since 2.1
 */
public class Main {

    public static void main(String[] args) throws Throwable {
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(
                "/org/springframework/integration/gemfire/store/messagegroupstore/GemfireMessageStore-context.xml");
		Producer producer = classPathXmlApplicationContext.getBean(Producer.class);
		for(int i =0 ;  i < 10 ; i++ ) {
			producer.sendManyMessages(i, Arrays.asList("1,2,3,4,5".split(",")));
		}
		Thread.sleep( 1000 * 10 );
    }

}
