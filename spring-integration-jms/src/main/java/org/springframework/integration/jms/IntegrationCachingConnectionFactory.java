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
package org.springframework.integration.jms;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;

import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.SessionProxy;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
/**
 * Temporary extension to {@link CachingConnectionFactory}. It allows you to
 * cache consummers for temporary queues.
 * This class will be eliminated once https://jira.springsource.org/browse/SPR-9648 is addressed.
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class IntegrationCachingConnectionFactory extends CachingConnectionFactory {

	@Override
	protected Session getCachedSessionProxy(Session target, LinkedList<Session> sessionList) {
		List<Class<?>> classes = new ArrayList<Class<?>>(3);
		classes.add(SessionProxy.class);
		if (target instanceof QueueSession) {
			classes.add(QueueSession.class);
		}
		if (target instanceof TopicSession) {
			classes.add(TopicSession.class);
		}
		return (Session) Proxy.newProxyInstance(
				SessionProxy.class.getClassLoader(),
				classes.toArray(new Class[classes.size()]),
				new CachedSessionInvocationHandler(target, sessionList));
	}

	private boolean isActive(){
		Field field = ReflectionUtils.findField(CachingConnectionFactory.class, "active");
		field.setAccessible(true);
		try {
			return (Boolean) field.get(this);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to check for 'active' status");
		}
	}

	private class CachedSessionInvocationHandler implements InvocationHandler {

		private final Session target;

		private final LinkedList<Session> sessionList;

		private final Map<DestinationCacheKey, MessageProducer> cachedProducers =
				new HashMap<DestinationCacheKey, MessageProducer>(10);

		private final Map<ConsumerCacheKey, MessageConsumer> cachedConsumers =
				new HashMap<ConsumerCacheKey, MessageConsumer>(10);

		private boolean transactionOpen = false;

		public CachedSessionInvocationHandler(Session target, LinkedList<Session> sessionList) {
			this.target = target;
			this.sessionList = sessionList;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
		    if (methodName.startsWith("create")) {
				this.transactionOpen = true;
				if (isCacheProducers() && (methodName.equals("createProducer") ||
						methodName.equals("createSender") || methodName.equals("createPublisher"))) {
					// Destination argument being null is ok for a producer
					return getCachedProducer((Destination) args[0]);
				}
				else if (isCacheConsumers()) {
					// let raw JMS invocation throw an exception if Destination (i.e. args[0]) is null
					if ((methodName.equals("createConsumer") || methodName.equals("createReceiver") ||
							methodName.equals("createSubscriber"))) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 1 ? (String) args[1] : null),
									(args.length > 2 && (Boolean) args[2]),
									null);
						}
					}
					else if (methodName.equals("createDurableSubscriber")) {
						Destination dest = (Destination) args[0];
						if (dest != null) {
							return getCachedConsumer(dest,
									(args.length > 2 ? (String) args[2] : null),
									(args.length > 3 && (Boolean) args[3]),
									(String) args[1]);
						}
					}
				}
			}
		    else if (methodName.equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (methodName.equals("hashCode")) {
				// Use hashCode of Session proxy.
				return System.identityHashCode(proxy);
			}
			else if (methodName.equals("toString")) {
				return "Cached JMS Session: " + this.target;
			}
			else if (methodName.equals("close")) {
				// Handle close method: don't pass the call on.
				if (isActive()) {
					synchronized (this.sessionList) {
						if (this.sessionList.size() < getSessionCacheSize()) {
							logicalClose((Session) proxy);
							// Remain open in the session list.
							return null;
						}
					}
				}
				// If we get here, we're supposed to shut down.
				physicalClose();
				return null;
			}
			else if (methodName.equals("getTargetSession")) {
				// Handle getTargetSession method: return underlying Session.
				return this.target;
			}
			else if (methodName.equals("commit") || methodName.equals("rollback")) {
				this.transactionOpen = false;
			}

			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private MessageProducer getCachedProducer(Destination dest) throws JMSException {
			DestinationCacheKey cacheKey = (dest != null ? new DestinationCacheKey(dest) : null);
			MessageProducer producer = this.cachedProducers.get(cacheKey);
			if (producer != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Found cached JMS MessageProducer for destination [" + dest + "]: " + producer);
				}
			}
			else {
				producer = this.target.createProducer(dest);
				if (logger.isDebugEnabled()) {
					logger.debug("Creating cached JMS MessageProducer for destination [" + dest + "]: " + producer);
				}
				this.cachedProducers.put(cacheKey, producer);
			}
			//return new CachedMessageProducer(producer);
			return createCachedMessageProducer(producer);
		}

		private MessageConsumer getCachedConsumer(
				Destination dest, String selector, boolean noLocal, String subscription) throws JMSException {

			ConsumerCacheKey cacheKey = new ConsumerCacheKey(dest, selector, noLocal, subscription);
			MessageConsumer consumer = this.cachedConsumers.get(cacheKey);
			if (consumer != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Found cached JMS MessageConsumer for destination [" + dest + "]: " + consumer);
				}
			}
			else {
				if (dest instanceof Topic) {
					consumer = (subscription != null ?
							this.target.createDurableSubscriber((Topic) dest, subscription, selector, noLocal) :
							this.target.createConsumer(dest, selector, noLocal));
				}
				else {
					consumer = this.target.createConsumer(dest, selector);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Creating cached JMS MessageConsumer for destination [" + dest + "]: " + consumer);
				}
				this.cachedConsumers.put(cacheKey, consumer);
			}
			//return new CachedMessageConsumer(consumer);
			return createCachedMessageConsumer(consumer);
		}

		private void logicalClose(Session proxy) throws JMSException {
			// Preserve rollback-on-close semantics.
			if (this.transactionOpen && this.target.getTransacted()) {
				this.transactionOpen = false;
				this.target.rollback();
			}
			// Physically close durable subscribers at time of Session close call.
			for (Iterator<Map.Entry<ConsumerCacheKey, MessageConsumer>> it = this.cachedConsumers.entrySet().iterator(); it.hasNext();) {
				Map.Entry<ConsumerCacheKey, MessageConsumer> entry = it.next();
				if (entry.getKey().subscription != null) {
					entry.getValue().close();
					it.remove();
				}
			}
			// Allow for multiple close calls...
			boolean returned = false;
			synchronized (this.sessionList) {
				if (!this.sessionList.contains(proxy)) {
					this.sessionList.addLast(proxy);
					returned = true;
				}
			}
			if (returned && logger.isTraceEnabled()) {
				logger.trace("Returned cached Session: " + this.target);
			}
		}

		private void physicalClose() throws JMSException {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing cached Session: " + this.target);
			}
			// Explicitly close all MessageProducers and MessageConsumers that
			// this Session happens to cache...
			try {
				for (MessageProducer producer : this.cachedProducers.values()) {
					producer.close();
				}
				for (MessageConsumer consumer : this.cachedConsumers.values()) {
					consumer.close();
				}
			}
			finally {
				this.cachedProducers.clear();
				this.cachedConsumers.clear();
				// Now actually close the Session.
				this.target.close();
			}
		}
	}

	private static class DestinationCacheKey {

		private final Destination destination;

		private String destinationString;

		public DestinationCacheKey(Destination destination) {
			Assert.notNull(destination, "Destination must not be null");
			this.destination = destination;
		}

		private String getDestinationString() {
			if (this.destinationString == null) {
				this.destinationString = this.destination.toString();
			}
			return this.destinationString;
		}

		protected boolean destinationEquals(DestinationCacheKey otherKey) {
			return (this.destination.getClass().equals(otherKey.destination.getClass()) &&
					(this.destination.equals(otherKey.destination) ||
							getDestinationString().equals(otherKey.getDestinationString())));
		}

		@Override
		public boolean equals(Object other) {
			// Effectively checking object equality as well as toString equality.
			// On WebSphere MQ, Destination objects do not implement equals...
			return (other == this || destinationEquals((DestinationCacheKey) other));
		}

		@Override
		public int hashCode() {
			// Can't use a more specific hashCode since we can't rely on
			// this.destination.hashCode() actually being the same value
			// for equivalent destinations... Thanks a lot, WebSphere MQ!
			return this.destination.getClass().hashCode();
		}
	}


	/**
	 * Simple wrapper class around a Destination and other consumer attributes.
	 * Used as the cache key when caching MessageConsumer objects.
	 */
	private static class ConsumerCacheKey extends DestinationCacheKey {

		private final String selector;

		private final boolean noLocal;

		private final String subscription;

		public ConsumerCacheKey(Destination destination, String selector, boolean noLocal, String subscription) {
			super(destination);
			this.selector = selector;
			this.noLocal = noLocal;
			this.subscription = subscription;
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) {
				return true;
			}
			ConsumerCacheKey otherKey = (ConsumerCacheKey) other;
			return (destinationEquals(otherKey) &&
					ObjectUtils.nullSafeEquals(this.selector, otherKey.selector) &&
					this.noLocal == otherKey.noLocal &&
					ObjectUtils.nullSafeEquals(this.subscription, otherKey.subscription));
		}
	}


	//============
	private MessageProducer createCachedMessageProducer(MessageProducer messageProducer){
		try {
			Class<?> cachedProducerClass =
					Class.forName("org.springframework.jms.connection.CachedMessageProducer",
							true, Thread.currentThread().getContextClassLoader());
			Constructor<?> c = cachedProducerClass.getConstructor(MessageProducer.class);
			c.setAccessible(true);
			return (MessageProducer) c.newInstance(messageProducer);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create org.springframework.jms.connection.CachedMessageProducer", e);
		}
	}

	private MessageConsumer createCachedMessageConsumer(MessageConsumer messageConsumer){
		try {
			Class<?> cachedProducerClass =
					Class.forName("org.springframework.jms.connection.CachedMessageConsumer",
							true, Thread.currentThread().getContextClassLoader());
			Constructor<?> c = cachedProducerClass.getConstructor(MessageConsumer.class);
			c.setAccessible(true);
			return (MessageConsumer) c.newInstance(messageConsumer);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create org.springframework.jms.connection.CachedMessageConsumer", e);
		}
	}
}
