/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.selector;

import java.util.stream.Collectors;

import org.springframework.core.log.LogAccessor;
import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
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
 * <p>
 * Each rejection is logged at {@code INFO} level with the ID of the message and the
 * validation errors (the field names and the error messages, but not the rejected values).
 *
 * @author Sharang Gupta
 * @author Artem Bilan
 *
 * @since 7.2
 *
 * @see Validator
 * @see SmartValidator
 * @see org.springframework.integration.filter.MessageFilter
 */
public class PayloadValidatingMessageSelector implements MessageSelector {

	private final LogAccessor logger = new LogAccessor(getClass());

	private final Validator validator;

	private Object[] validationHints = {};

	private boolean acceptUnsupportedPayloads;

	/**
	 * Create a selector for the provided validator.
	 * @param validator the validator to apply to the message payload.
	 */
	public PayloadValidatingMessageSelector(Validator validator) {
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
		this.validationHints = validationHints.clone();
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
		Object payload = message.getPayload();
		if (!this.validator.supports(payload.getClass())) {
			if (!this.acceptUnsupportedPayloads) {
				this.logger.info(() -> "The message [" + message.getHeaders().getId()
						+ "] has been rejected: the validator does not support the payload type "
						+ payload.getClass().getName());
			}
			return this.acceptUnsupportedPayloads;
		}
		Errors errors = new SimpleErrors(payload);
		if (this.validator instanceof SmartValidator smartValidator) {
			smartValidator.validate(payload, errors, this.validationHints);
		}
		else {
			this.validator.validate(payload, errors);
		}
		if (errors.hasErrors()) {
			this.logger.info(() -> "The message [" + message.getHeaders().getId()
					+ "] has been rejected by the payload validation: " + describe(errors));
			return false;
		}
		return true;
	}

	/**
	 * Describe the validation errors without the rejected values,
	 * which may carry sensitive data.
	 */
	private static String describe(Errors errors) {
		return errors.getAllErrors()
				.stream()
				.map(PayloadValidatingMessageSelector::describe)
				.collect(Collectors.joining("; "));
	}

	private static String describe(ObjectError error) {
		String defaultMessage = error.getDefaultMessage();
		String reason = String.valueOf(defaultMessage != null ? defaultMessage : error.getCode());
		if (error instanceof FieldError fieldError) {
			return "'" + fieldError.getField() + "': " + reason;
		}
		return reason;
	}

}
