/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.support;

import java.util.Collection;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A {@link MessagingException} thrown when a non-transactional operation is
 * performing multiple updates from a single message, e.g. an FTP 'mput' operation.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class PartialSuccessException extends MessagingException {

	private static final long serialVersionUID = 8810900575763284993L;

	private final Collection<?> partialResults;

	private final Collection<?> derivedInput;

	/**
	 *
	 * @param message the message.
	 * @param description the description.
	 * @param cause the cause.
	 * @param partialResults The subset of multiple updates that were successful before the cause occurred.
	 * @param derivedInput The collection (usually derived from the message) of input data; e.g. a filtered
	 * list of local files being sent to FTP using {@code mput}.
	 */
	public PartialSuccessException(Message<?> message, String description, Throwable cause,
			Collection<?> partialResults, Collection<?> derivedInput) {
		super(message, description, cause);
		Assert.notNull(cause, "Cause is required");
		this.partialResults = partialResults;
		this.derivedInput = derivedInput;
	}

	/**
	 * See {@link #PartialSuccessException(Message, String, Throwable, Collection, Collection)}.
	 * @return the partial results
	 */
	public Collection<?> getPartialResults() {
		return this.partialResults;
	}

	/**
	 * See {@link #PartialSuccessException(Message, String, Throwable, Collection, Collection)}.
	 * @return the derived input.
	 */
	public Collection<?> getDerivedInput() {
		return this.derivedInput;
	}

	/**
	 * Convenience version of {@link #getPartialResults()} to avoid casting.
	 * @param clazz the type.
	 * @param <T> the result type.
	 * @return the partial results.
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getPartialResults(Class<T> clazz) {
		return (Collection<T>) this.partialResults;
	}

	/**
	 * Convenience version of {@link #getDerivedInput()} to avoid casting.
	 * @param clazz the type.
	 * @param <T> the type of input.
	 * @return the partial results.
	 */
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getDerivedInput(Class<T> clazz) {
		return (Collection<T>) this.derivedInput;
	}

	@Override
	public String toString() {
		return "PartialSuccessException [" + getMessage() + ":" + getCause().getMessage()
				+ ", partialResults=" + this.partialResults + ", derivedInput=" + this.derivedInput
				+ ", failedMessage=" + getFailedMessage() + "]";
	}

}
