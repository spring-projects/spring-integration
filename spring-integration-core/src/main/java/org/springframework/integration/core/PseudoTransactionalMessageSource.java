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
package org.springframework.integration.core;

import org.springframework.transaction.support.TransactionSynchronization;

/**
 * {@link MessageSource}s implementing this sub-interface can participate in
 * a Spring transaction. While the underlying resource is not strictly
 * transactional, the final disposition of the resource will be
 * synchronized with any encompassing transaction. For example, when
 * a message source is used with a transactional poller, if any upstream
 * activity causes the transaction to roll back, then the {@link #afterRollback(Object)}
 * method will be called, allowing the message source to reset the state of
 * whatever. If the transaction commits, the {@link #afterCommit(Object)} method
 * is called.<p/>
 * For example, with a MailReceivingMessageSource, the email can be deleted
 * on successful commit, but not deleted if the transaction rolls back.
 * <p/>
 * This implements the 'Best Chance 1PC' pattern where there is only a
 * small (but present) window in which a transaction might commit but the
 * resource is not updated to reflect that. This could result in
 * duplicate messages.
 * @author Gary Russell
 * @since 2.2
 *
 */
public interface PseudoTransactionalMessageSource<T> extends MessageSource<T> {

	/**
	 * Obtain the resource on which appropriate action needs
	 * to be taken.
	 * @return The resource.
	 */
	Object getResource();

	/**
	 * Invoked via {@link TransactionSynchronization} when the
	 * transaction commits.
	 * @param resource The resource to be "committed"
	 */
	void afterCommit(Object resource);

	/**
	 * Invoked via {@link TransactionSynchronization} when the
	 * transaction rolls back.
	 * @param resource
	 */
	void afterRollback(Object resource);

	/**
	 * Called when there is no transaction and the receive() call completed.
	 * @param resource
	 */
	void afterReceiveNoTX(Object resource);

	/**
	 * Called when there is no transaction and after the message was
	 * sent to the channel.
	 * @param resource
	 */
	void afterSendNoTX(Object resource);

}
