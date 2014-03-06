/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.inbound;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Polling message source that produces messages from the result of the provided:
 *
 * <ul>
 *     <li>entityClass</li>
 *     <li>JpQl Select Query</li>
 *     <li>Sql Native Query</li>
 *     <li>JpQl Named Query</li>
 *     <li>Sql Native Named Query</li>
 * </ul>
 *
 * After the objects have been polled, it also possibly to either:
 *
 * executes an update after the select possibly to updated the state of selected records
 *
 * <ul>
 *     <li>executes an update (per retrieved object or for the entire payload)</li>
 *     <li>delete the retrieved object</li>
 * </ul>
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 *
 * @since 2.2
 *
 */
public class JpaPollingChannelAdapter extends IntegrationObjectSupport implements MessageSource<Object>{

	private final JpaExecutor jpaExecutor;

	/**
	 * Constructor taking a {@link JpaExecutor} that provide all required JPA
	 * functionality.
	 *
	 * @param jpaExecutor Must not be null.
	 */
	public JpaPollingChannelAdapter(JpaExecutor jpaExecutor) {
		super();
		Assert.notNull(jpaExecutor, "jpaExecutor must not be null.");
		this.jpaExecutor = jpaExecutor;
	}

	/**
	 * Check for mandatory attributes
	 */
	@Override
	protected void onInit() throws Exception {
		 super.onInit();
		 this.jpaExecutor.setBeanFactory(this.getBeanFactory());
	}

	/**
	 * Uses {@link JpaExecutor#poll()} to executes the JPA operation.
	 *
	 * If {@link JpaExecutor#poll()} returns null, this method will return
	 * <code>null</code>. Otherwise, a new {@link Message} is constructed and returned.
	 */
	public Message<Object> receive() {

		final Object payload = jpaExecutor.poll();

		if (payload == null) {
			return null;
		}

		return this.getMessageBuilderFactory().withPayload(payload).build();
	}

	@Override
	public String getComponentType(){
		return "jpa:inbound-channel-adapter";
	}

}
