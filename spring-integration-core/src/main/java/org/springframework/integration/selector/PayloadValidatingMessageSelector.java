/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.selector;

import java.util.Arrays;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.SimpleErrors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

/**
 * The {@link MessageSelector} implementation to accept a message only if its payload
 * passes the validation of the provided {@link Validator}.
 * <p>
 * A Bean Validation (JSR-303) provider can be plugged in through the
 * {@code org.springframework.validation.beanvalidation.LocalValidatorFactoryBean},
 * which exposes it as a Spring {@link Validator}.
 * <p>
 * Used with a {@link org.springframework.integration.filter.MessageFilter}, messages
 * with an invalid payload are discarded, sent to the discard channel or rejected with
 * an exception, according to the filter configuration.
 * <p>
 * A payload the validator does not {@link Validator#supports(Class) support} is rejected
 * by default; set {@link #setAcceptUnsupportedPayloads(boolean)} to {@code true} to accept
 * such messages instead.
 *
 * @author Sharang Gupta
 *
 * @since 7.2
 *
 * @see Validator
 * @see SmartValidator
 * @see org.springframework.integration.filter.MessageFilter
 */
public class PayloadValidatingMessageSelector implements MessageSelector {

	private final Validator validator;

	private Object[] validationHints = {};

	private boolean acceptUnsupportedPayloads;

	/**
	 * Create a selector for the provided validator.
	 * @param validator the validator to apply to the message payload.
	 */
	public PayloadValidatingMessageSelector(Validator validator) {
		Assert.notNull(validator, "'validator' must not be null");
		this.validator = validator;
	}

	/**
	 * Set the validation hints to pass to a {@link SmartValidator},
	 * for example Bean Validation groups.
	 * Ignored when the validator is not a {@link SmartValidator}.
	 * @param validationHints the validation hints.
	 * @see SmartValidator#validate(Object, Errors, Object...)
	 */
	public void setValidationHints(Object... validationHints) {
		Assert.notNull(validationHints, "'validationHints' must not be null");
		this.validationHints = Arrays.copyOf(validationHints, validationHints.length);
	}

	/**
	 * Whether a payload the validator does not {@link Validator#supports(Class) support} is accepted.
	 * @param acceptUnsupportedPayloads true to accept such payloads;
	 * false (the default) to reject them.
	 */
	public void setAcceptUnsupportedPayloads(boolean acceptUnsupportedPayloads) {
		this.acceptUnsupportedPayloads = acceptUnsupportedPayloads;
	}

	@Override
	public boolean accept(Message<?> message) {
		Assert.notNull(message, "'message' must not be null");
		Object payload = message.getPayload();
		if (!this.validator.supports(payload.getClass())) {
			return this.acceptUnsupportedPayloads;
		}
		Errors errors = new SimpleErrors(payload);
		if (this.validator instanceof SmartValidator smartValidator) {
			smartValidator.validate(payload, errors, this.validationHints);
		}
		else {
			this.validator.validate(payload, errors);
		}
		return !errors.hasErrors();
	}

}
