/*
 * Copyright 2016-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms.dsl;

import javax.jms.Destination;
import javax.jms.ExceptionListener;

import org.springframework.beans.BeanUtils;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.util.ErrorHandler;

/**
 * A {@link JmsDestinationAccessorSpec} for {@link JmsListenerContainerSpec}s.
 *
 * @param <S> the target {@link JmsListenerContainerSpec} implementation type.
 * @param <C> the target {@link AbstractMessageListenerContainer} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class JmsListenerContainerSpec<S extends JmsListenerContainerSpec<S, C>,
		C extends AbstractMessageListenerContainer>
		extends JmsDestinationAccessorSpec<S, C> {

	protected JmsListenerContainerSpec(Class<C> aClass) {
		super(BeanUtils.instantiateClass(aClass));
		if (DefaultMessageListenerContainer.class.isAssignableFrom(aClass)) {
			this.target.setSessionTransacted(true);
		}
	}

	/**
	 * @param destination the destination.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setDestination(Destination)
	 */
	S destination(Destination destination) {
		this.target.setDestination(destination);
		return _this();
	}

	/**
	 * @param destinationName the destinationName.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setDestinationName(String)
	 */
	S destination(String destinationName) {
		this.target.setDestinationName(destinationName);
		return _this();
	}

	/**
	 * @param messageSelector the messageSelector.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setMessageSelector(String)
	 */
	public S messageSelector(String messageSelector) {
		this.target.setMessageSelector(messageSelector);
		return _this();
	}

	/**
	 * @param subscriptionDurable the subscriptionDurable.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setSubscriptionDurable(boolean)
	 */
	public S subscriptionDurable(boolean subscriptionDurable) {
		this.target.setSubscriptionDurable(subscriptionDurable);
		return _this();
	}

	/**
	 * @param durableSubscriptionName the durableSubscriptionName.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setDurableSubscriptionName(String)
	 */
	public S durableSubscriptionName(String durableSubscriptionName) {
		this.target.setDurableSubscriptionName(durableSubscriptionName);
		return _this();
	}

	/**
	 * Set whether to make the subscription shared.
	 * @param subscriptionShared make the subscription shared or not.
	 * @return the spec.
	 * @since 5.0.7
	 * @see AbstractMessageListenerContainer#setSubscriptionShared(boolean)
	 */
	public S subscriptionShared(boolean subscriptionShared) {
		this.target.setSubscriptionShared(subscriptionShared);
		return _this();
	}

	/**
	 * @param exceptionListener the exceptionListener.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setExceptionListener(ExceptionListener)
	 */
	public S exceptionListener(ExceptionListener exceptionListener) {
		this.target.setExceptionListener(exceptionListener);
		return _this();
	}

	/**
	 * @param errorHandler the errorHandler.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setErrorHandler(ErrorHandler)
	 */
	public S errorHandler(ErrorHandler errorHandler) {
		this.target.setErrorHandler(errorHandler);
		return _this();
	}

	/**
	 * @param exposeListenerSession the exposeListenerSession.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setExposeListenerSession(boolean)
	 */
	public S exposeListenerSession(boolean exposeListenerSession) {
		this.target.setExposeListenerSession(exposeListenerSession);
		return _this();
	}

	/**
	 * @param acceptMessagesWhileStopping the acceptMessagesWhileStopping.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setAcceptMessagesWhileStopping(boolean)
	 */
	public S acceptMessagesWhileStopping(boolean acceptMessagesWhileStopping) {
		this.target.setAcceptMessagesWhileStopping(acceptMessagesWhileStopping);
		return _this();
	}

	/**
	 * @param clientId the clientId.
	 * @return the spec.
	 * @see AbstractMessageListenerContainer#setClientId(String)
	 */
	public S clientId(String clientId) {
		this.target.setClientId(clientId);
		return _this();
	}

}
