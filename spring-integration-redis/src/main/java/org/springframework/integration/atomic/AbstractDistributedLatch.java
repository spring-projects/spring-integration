/*
 * Copyright 2007-2011 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.atomic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Oleg Zhurakousky
 *
 */
public abstract class AbstractDistributedLatch implements DistributeLatch{

	private final CountDownLatch latch;
	
	private final SubscribableChannel notificationChannel;
	
	private final String correlationId;
	
	public AbstractDistributedLatch(int count, String correlationId, SubscribableChannel notificationChannel){
		this.latch = new CountDownLatch(count);
		this.notificationChannel = notificationChannel;
		this.correlationId = correlationId;
		this.notificationChannel.subscribe(new NotificationHandler());
	}
	
	public boolean await() throws InterruptedException {
		latch.await();
		return this.aquireLock();
	}

	
	public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
		latch.await(timeout, timeUnit);
		return this.aquireLock();
	}

	
	public void countDown() {
		if (this.releaseLock()){
			Message<?> notificationMessage = new GenericMessage<String>(correlationId);
			this.notificationChannel.send(notificationMessage);
		}
		latch.countDown();
	}
	
	
	protected abstract boolean aquireLock();
	
	protected abstract boolean releaseLock();
	
	private class NotificationHandler implements MessageHandler{
		
		public void handleMessage(Message<?> message){
			countDown();
		}
	}

}
