package org.springframework.integration.atomic;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.channel.PublishSubscribeChannel;

public class SpringIntegrationDistributedLatch extends AbstractDistributedLatch{
	
	private final AtomicInteger distributedLock;
	
	public SpringIntegrationDistributedLatch(int count, String correlationId){
		super(count, correlationId, NotifierFactory.getNotifier(correlationId));
		this.distributedLock = LockFactory.getLock(correlationId);
	}
	
	@Override
	protected boolean aquireLock() {
		return this.distributedLock.compareAndSet(0, 1);
	}

	@Override
	protected boolean releaseLock() {
		return this.distributedLock.compareAndSet(1, 0);
	}
	
	private static class NotifierFactory{
		
		private static final Map<String, PublishSubscribeChannel> notifierMap = new ConcurrentHashMap<String, PublishSubscribeChannel>();
		
		public static PublishSubscribeChannel getNotifier(String correlationId){
			if (notifierMap.containsKey(correlationId)){
				return notifierMap.get(correlationId);
			}
			else {
				PublishSubscribeChannel notificationChannel = new PublishSubscribeChannel();
				notifierMap.put(correlationId, notificationChannel);
				return notificationChannel;
			}
		}
	}
	
	private static class LockFactory{
		
		private static final Map<String, AtomicInteger> lockMap = new ConcurrentHashMap<String, AtomicInteger>();
		
		public static AtomicInteger getLock(String correlationId){
			if (lockMap.containsKey(correlationId)){
				return lockMap.get(correlationId);
			}
			else {
				AtomicInteger lock = new AtomicInteger();
				lockMap.put(correlationId, lock);
				return lock;
			}
		}
	}
}
