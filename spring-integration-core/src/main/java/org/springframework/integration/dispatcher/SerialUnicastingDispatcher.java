/* Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.Assert;
/**
 * This variation of {@link MessageDispatcher} is based on {@link UnicastingDispatcher}.
 * However this dispatcher will dispatch Messages that have the same 'correlationId' headers serially and 
 * in sequence (based on the 'sequenceNumber' header) while Messages with different 'correlationId' will be 
 * dispatched concurrently. 
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class SerialUnicastingDispatcher extends AbstractDispatcher {
	
	private final ConcurrentHashMap<Object, List<CountDownLatch>> latches = new ConcurrentHashMap<Object, List<CountDownLatch>>();
	
	private final Map<Object, Integer> messageSequence = new HashMap<Object, Integer>();
	
	private final Executor executor;

	public SerialUnicastingDispatcher(Executor executor) {
		this.executor = executor;
	}

	protected void preDispatch(Message<?> message) {
		Object correlationId = message.getHeaders().get(MessageHeaders.CORRELATION_ID);
		
		try {
			
			Integer currentSequenceNumber = message.getHeaders().getSequenceNumber();
			Assert.notNull(currentSequenceNumber, "'sequenceNumber' must not be null");
			
			CountDownLatch latch = null;
			
			synchronized (correlationId) {
				Integer lastSequenceNumber = messageSequence.get(correlationId);
				
				if (lastSequenceNumber == null){ //nothing was processed in this sequence - starting
					lastSequenceNumber = 0;
					messageSequence.put(correlationId, lastSequenceNumber);
				}
				
				if (currentSequenceNumber > 0){				
					if (lastSequenceNumber < currentSequenceNumber){	
						int latchSize = currentSequenceNumber - lastSequenceNumber;
						latch = new CountDownLatch(latchSize);
						List<CountDownLatch> listOfLatches = this.aquireLatches(correlationId);
						listOfLatches.add(latch);
					}
				}	
			}
			
			if (latch != null){
				if (this.logger.isDebugEnabled()){
					this.logger.debug("Waiting to dispatch out of sequence Message: " + message);
				}
				latch.await();
				synchronized (correlationId) {
					List<CountDownLatch> listOfLatches = this.aquireLatches(correlationId);
					listOfLatches.remove(latch);
					messageSequence.put(correlationId, currentSequenceNumber);
				}
			}

		} catch (InterruptedException e) {
			throw new MessagingException(message, "Message dispatching is interrupted", e);
		}
	}

	protected void postDispatch(Message<?> message) {
		Object correlationId = message.getHeaders().get(MessageHeaders.CORRELATION_ID);
		Integer sequenceNumber = message.getHeaders().getSequenceNumber();
		
		synchronized (correlationId) {				
			messageSequence.put(correlationId, sequenceNumber+1);
			List<CountDownLatch> listOfLatches = this.aquireLatches(correlationId);
			for (CountDownLatch countDownLatch : listOfLatches) {
				countDownLatch.countDown();
			}
			
		}
	}
	
	private List<CountDownLatch> aquireLatches(Object correlationId){
		Assert.notNull(correlationId, "'correlationId' must not be null");
		if (correlationId instanceof String){
			Assert.hasText((String) correlationId, "'correlationId' must not be empty");
		}
		List<CountDownLatch> listOfLatches = latches.get(correlationId);
		if (listOfLatches == null){
			listOfLatches = new CopyOnWriteArrayList<CountDownLatch>();
			latches.put(correlationId, listOfLatches);
		}
		return listOfLatches;
	}
	
	// !!!!! copied from UnicastingDispatcher
	// With small refactoring to the UnicastingDispatcher, this code shoudl be eliminated
	private volatile LoadBalancingStrategy loadBalancingStrategy;
	
	private volatile boolean failover = true;
	
	private ReadWriteLock rwLock = new ReentrantReadWriteLock();
	
	public void setFailover(boolean failover) {
		this.failover = failover;
	}
	
	public void setLoadBalancingStrategy(LoadBalancingStrategy loadBalancingStrategy) {
		Lock lock = rwLock.writeLock();
		lock.lock();
		this.loadBalancingStrategy = loadBalancingStrategy;
		lock.unlock();
	}
	
	
	private boolean doDispatch(Message<?> message) {
		this.preDispatch(message);
		
		boolean success = false;
		Iterator<MessageHandler> handlerIterator = this.getHandlerIterator(message);
		if (!handlerIterator.hasNext()) {
			throw new MessageDeliveryException(message, "Dispatcher has no subscribers.");
		}
		List<RuntimeException> exceptions = new ArrayList<RuntimeException>();
		while (success == false && handlerIterator.hasNext()) {
			MessageHandler handler = handlerIterator.next();
			try {
				handler.handleMessage(message);
				success = true; // we have a winner.
			}
			catch (Exception e) {
				RuntimeException runtimeException = (e instanceof RuntimeException)
						? (RuntimeException) e
						: new MessageDeliveryException(message,
								"Dispatcher failed to deliver Message.", e);
				if (e instanceof MessagingException &&
						((MessagingException) e).getFailedMessage() == null) {
					((MessagingException) e).setFailedMessage(message);
				}
				exceptions.add(runtimeException);
				this.handleExceptions(exceptions, message, !handlerIterator.hasNext());
			}
		}
		this.postDispatch(message);
		return success;
	}
	
	public boolean dispatch(final Message<?> message) {
		if (this.executor != null) {
			this.executor.execute(new Runnable() {
				public void run() {
					//System.out.println(message);
					doDispatch(message);
				}
			});
			return true;
		}
		return this.doDispatch(message);
	}
	
	private Iterator<MessageHandler> getHandlerIterator(Message<?> message) {
		Lock lock = rwLock.readLock();
		lock.lock();
		try {
			if (this.loadBalancingStrategy != null) {
				return this.loadBalancingStrategy.getHandlerIterator(message, this.getHandlers());
			}
		} finally {
			lock.unlock();
		}	
		return this.getHandlers().iterator();
	}
	
	private void handleExceptions(List<RuntimeException> allExceptions, Message<?> message, boolean isLast) {
		if (isLast || !this.failover) {
			if (allExceptions != null && allExceptions.size() == 1) {
				throw allExceptions.get(0);
			}
			throw new AggregateMessageDeliveryException(message,
					"All attempts to deliver Message to MessageHandlers failed.", allExceptions);
		}
	}
}
