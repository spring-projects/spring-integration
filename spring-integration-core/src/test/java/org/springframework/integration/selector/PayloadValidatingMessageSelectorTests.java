/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.selector;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Sharang Gupta
 *
 * @since 7.2
 */
class PayloadValidatingMessageSelectorTests {

	private static final Validator NON_BLANK_STRING_VALIDATOR = new Validator() {

		@Override
		public boolean supports(Class<?> clazz) {
			return String.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			if (((String) target).isBlank()) {
				errors.reject("blank", "The payload must not be blank");
			}
		}

	};

	@Test
	void validPayloadIsAccepted() {
		PayloadValidatingMessageSelector selector = new PayloadValidatingMessageSelector(NON_BLANK_STRING_VALIDATOR);

		assertThat(selector.accept(new GenericMessage<>("order"))).isTrue();
	}

	@Test
	void invalidPayloadIsRejected() {
		PayloadValidatingMessageSelector selector = new PayloadValidatingMessageSelector(NON_BLANK_STRING_VALIDATOR);

		assertThat(selector.accept(new GenericMessage<>("   "))).isFalse();
	}

	@Test
	void unsupportedPayloadIsRejectedUnlessConfiguredOtherwise() {
		PayloadValidatingMessageSelector selector = new PayloadValidatingMessageSelector(NON_BLANK_STRING_VALIDATOR);
		Message<Integer> message = new GenericMessage<>(42);

		assertThat(selector.accept(message)).isFalse();

		selector.setAcceptUnsupportedPayloads(true);

		assertThat(selector.accept(message)).isTrue();
	}

	@Test
	void validationHintsArePassedToSmartValidator() {
		List<Object> receivedHints = new java.util.ArrayList<>();
		SmartValidator smartValidator = new SmartValidator() {

			@Override
			public boolean supports(Class<?> clazz) {
				return true;
			}

			@Override
			public void validate(Object target, Errors errors) {
				errors.reject("plainValidateMustNotBeCalled");
			}

			@Override
			public void validate(Object target, Errors errors, Object... validationHints) {
				receivedHints.addAll(List.of(validationHints));
			}

		};
		PayloadValidatingMessageSelector selector = new PayloadValidatingMessageSelector(smartValidator);
		selector.setValidationHints("group1", "group2");

		assertThat(selector.accept(new GenericMessage<>("order"))).isTrue();
		assertThat(receivedHints).containsExactly("group1", "group2");
	}

	@Test
	void validatorIsRequired() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new PayloadValidatingMessageSelector(null))
				.withMessage("'validator' must not be null");
	}

	@Test
	void invalidPayloadIsDiscardedByMessageFilter() {
		QueueChannel outputChannel = new QueueChannel();
		QueueChannel discardChannel = new QueueChannel();
		MessageFilter filter = new MessageFilter(new PayloadValidatingMessageSelector(NON_BLANK_STRING_VALIDATOR));
		filter.setOutputChannel(outputChannel);
		filter.setDiscardChannel(discardChannel);

		Message<String> validMessage = new GenericMessage<>("order");
		Message<String> invalidMessage = new GenericMessage<>("");
		filter.handleMessage(validMessage);
		filter.handleMessage(invalidMessage);

		assertThat(outputChannel.receive(0)).isSameAs(validMessage);
		assertThat(discardChannel.receive(0)).isSameAs(invalidMessage);
	}

}
