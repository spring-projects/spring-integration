/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Sharang Gupta
 *
 * @since 7.2
 */
class PayloadValidatingMessageSelectorTests {

	private static final Validator NON_BLANK_STRING_VALIDATOR =
			new Validator() {

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
		List<Object> receivedHints = new ArrayList<>();
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
	void rejectionIsLoggedWithValidationErrorsButNotRejectedValues() {
		Validator orderValidator = new Validator() {

			@Override
			public boolean supports(Class<?> clazz) {
				return Order.class.isAssignableFrom(clazz);
			}

			@Override
			public void validate(Object target, Errors errors) {
				errors.rejectValue("email", "invalid", "must be a well-formed email address");
				errors.reject("incomplete", "The order is incomplete");
			}

		};
		PayloadValidatingMessageSelector selector = new PayloadValidatingMessageSelector(orderValidator);
		LogAccessor logger = mock();
		when(logger.isInfoEnabled()).thenReturn(true);
		new DirectFieldAccessor(selector).setPropertyValue("logger", logger);

		Message<Order> message = new GenericMessage<>(new Order("secret-value"));

		assertThat(selector.accept(message)).isFalse();

		ArgumentCaptor<Supplier<CharSequence>> logMessage = ArgumentCaptor.captor();
		verify(logger).info(logMessage.capture());
		assertThat(logMessage.getValue().get().toString())
				.contains(message.getHeaders().getId().toString())
				.contains("'email': must be a well-formed email address")
				.contains("The order is incomplete")
				.doesNotContain("secret-value");
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

	record Order(String email) {

	}

}
