/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.converter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Glenn Renfro
 *
 * @since 7.2
 */
class AllowListDeserializingConverterTests {

	private static final String CUSTOM_PACKAGE_PATTERN = "org.springframework.integration.support.converter.*";

	private final AllowListDeserializingConverter converter = new AllowListDeserializingConverter();

	@Test
	void customTypeIsRejectedByDefault() {
		assertRejected(new CustomPayload("test"));
	}

	@Test
	void jdkTypeOutsideDefaultsIsRejected() {
		assertRejected(new ConcurrentHashMap<>(Map.of("key", "value")));
	}

	@Test
	void loggingTypeIsRejectedByDefault() {
		assertRejected(Level.INFO);
	}

	@Test
	void messageWithHistoryAndUuidHeaderRoundTripsWithDefaults() {
		Message<String> message =
				MessageHistory.write(new GenericMessage<>("payload", Map.of("uuid", UUID.randomUUID())),
						new TestComponent());

		Object result = this.converter.convert(serialize(message));

		assertThat(result).isInstanceOf(GenericMessage.class);
		Message<?> decoded = (Message<?>) result;
		assertThat(decoded.getPayload()).isEqualTo("payload");
		assertThat(decoded.getHeaders().get("uuid")).isEqualTo(message.getHeaders().get("uuid"));
		assertThat(MessageHistory.read(decoded)).hasSize(1);
	}

	@ParameterizedTest
	@MethodSource("defaultAllowedValues")
	void defaultAllowedTypeRoundTrips(Object value) {
		assertThat(this.converter.convert(serialize(value))).isEqualTo(value);
	}

	@Test
	void customNumberSubclassIsRejected() {
		assertRejected(new CustomNumber());
	}

	@Test
	void atomicLongIsRejectedByDefault() {
		assertRejected(new AtomicLong(1));
	}

	@Test
	void emptyArrayOfCustomTypeIsRejected() {
		assertRejected(new CustomPayload[0]);
	}

	@Test
	void arrayOfCustomTypeIsRejected() {
		assertRejected(new CustomPayload[] {new CustomPayload("test")});
	}

	@Test
	void arraysOfDefaultAllowedTypesRoundTrip() {
		assertThat((Object[]) this.converter.convert(serialize(new Integer[] {1, 2}))).containsExactly(1, 2);
		assertThat((String[][]) this.converter.convert(serialize(new String[][] {{"value"}})))
				.isDeepEqualTo(new String[][] {{"value"}});
		assertThat((int[]) this.converter.convert(serialize(new int[] {1, 2}))).containsExactly(1, 2);
	}

	@Test
	void objectArrayRoundTrips() {
		assertThat((Object[]) this.converter.convert(serialize(new Object[] {1, "value"})))
				.containsExactly(1, "value");
	}

	@ParameterizedTest
	@MethodSource("frameworkExceptions")
	void errorMessageWithFrameworkExceptionRoundTrips(MessagingException exception) {
		Object result = this.converter.convert(serialize(new ErrorMessage(exception)));

		assertThat(result).isInstanceOf(ErrorMessage.class);
		Throwable payload = ((ErrorMessage) result).getPayload();
		assertThat(payload).isExactlyInstanceOf(exception.getClass())
				.hasMessage(exception.getMessage());
		assertThat(((MessagingException) payload).getFailedMessage()).isNotNull();
	}

	static Stream<MessagingException> frameworkExceptions() {
		Message<String> failedMessage = new GenericMessage<>("payload");
		return Stream.of(
				new MessagingException(failedMessage, "messaging"),
				new MessageDeliveryException(failedMessage, "delivery"),
				new MessageHandlingException(failedMessage, "handling"),
				new MessageRejectedException(failedMessage, "rejected"),
				new MessageTimeoutException(failedMessage, "timeout"),
				new MessageDispatchingException(failedMessage, "dispatching"));
	}

	@ParameterizedTest
	@MethodSource("commonJdkExceptions")
	void errorMessageWithCommonJdkCauseRoundTrips(Exception cause) {
		Message<String> failedMessage = new GenericMessage<>("payload");
		MessageHandlingException exception = new MessageHandlingException(failedMessage, "handling", cause);

		Object result = this.converter.convert(serialize(new ErrorMessage(exception)));

		assertThat(result).isInstanceOfSatisfying(ErrorMessage.class, errorMessage ->
						assertThat(errorMessage.getPayload())
								.isExactlyInstanceOf(MessageHandlingException.class)
								.cause()
								.isExactlyInstanceOf(cause.getClass())
								.hasMessage(cause.getMessage()));
	}

	static Stream<Exception> commonJdkExceptions() {
		return Stream.of(
				new IllegalStateException("state"),
				new IllegalArgumentException("argument"),
				new NullPointerException("null"),
				new UnsupportedOperationException("unsupported"),
				new ClassCastException("cast"),
				new IndexOutOfBoundsException("index"),
				new IOException("io"),
				new UncheckedIOException("unchecked io", new IOException("io")),
				new TimeoutException("timeout"),
				new NoSuchElementException("no element"));
	}

	@Test
	void errorMessageWithCustomExceptionIsRejected() {
		assertRejected(new ErrorMessage(new CustomException()));
	}

	@Test
	void addAllowedPatternsKeepsDefaults() {
		this.converter.addAllowedPatterns(CUSTOM_PACKAGE_PATTERN);

		assertRoundTrips(new CustomPayload("test"));
		assertRoundTrips(new HashMap<>(Map.of("key", "value")));
	}

	@Test
	void setAllowedPatternsReplacesDefaults() {
		this.converter.setAllowedPatterns(CUSTOM_PACKAGE_PATTERN);

		assertRoundTrips(new CustomPayload("test"));
		assertRejected(new HashMap<>(Map.of("key", "value")));
	}

	@Test
	void wildcardAllowsAllTypes() {
		this.converter.setAllowedPatterns("*");

		assertRoundTrips(new ConcurrentHashMap<>(Map.of("key", "value")));
	}

	@Test
	void emptySetAllowedPatternsIsRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.converter.setAllowedPatterns())
				.withMessageContaining("must not be empty");

		assertRoundTrips(new HashMap<>(Map.of("key", "value")));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "  ")
	void setAllowedPatternsRejectsBlankPattern(String pattern) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.converter.setAllowedPatterns(pattern));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "  ")
	void addAllowedPatternsRejectsBlankPattern(String pattern) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.converter.addAllowedPatterns(pattern));
	}

	@Test
	void securityExceptionMessagePointsToAddAllowedPatterns() {
		byte[] bytes = serialize(new CustomPayload("test"));

		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> this.converter.convert(bytes))
				.havingRootCause()
				.isInstanceOf(SecurityException.class)
				.withMessageContaining(CustomPayload.class.getName())
				.withMessageContaining("addAllowedPatterns()");
	}

	static Stream<Object> defaultAllowedValues() {
		return Stream.of(
				new HashMap<>(Map.of("key", "value")),
				new LinkedHashMap<>(Map.of("key", "value")),
				new TreeMap<>(Map.of("key", "value")),
				new ArrayList<>(List.of("value")),
				new LinkedList<>(List.of("value")),
				new HashSet<>(Set.of("value")),
				new LinkedHashSet<>(Set.of("value")),
				new TreeSet<>(Set.of("value")),
				UUID.randomUUID(),
				new Date(),
				Arrays.asList("value"),
				List.of("value"),
				Set.of("value"),
				Map.of("key", "value"),
				Collections.singletonList("value"),
				Collections.singleton("value"),
				Collections.singletonMap("key", "value"),
				Collections.emptyList(),
				Collections.emptySet(),
				Collections.emptyMap(),
				Collections.unmodifiableList(new ArrayList<>(List.of("value"))),
				Collections.unmodifiableList(new LinkedList<>(List.of("value"))),
				Collections.unmodifiableSet(new HashSet<>(Set.of("value"))),
				Collections.unmodifiableSortedSet(new TreeSet<>(Set.of("value"))),
				Collections.unmodifiableMap(new HashMap<>(Map.of("key", "value"))),
				Collections.unmodifiableSortedMap(new TreeMap<>(Map.of("key", "value"))),
				Boolean.TRUE,
				'c',
				(byte) 1,
				(short) 1,
				1,
				1L,
				1.0f,
				1.0d,
				BigInteger.ONE,
				BigDecimal.ONE,
				Instant.now(),
				Duration.ofSeconds(1),
				new File("test.txt"));
	}

	private void assertRoundTrips(Object value) {
		assertThat(this.converter.convert(serialize(value))).isEqualTo(value);
	}

	private void assertRejected(Object value) {
		byte[] bytes = serialize(value);
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> this.converter.convert(bytes))
				.havingRootCause()
				.isInstanceOf(SecurityException.class);
	}

	private static byte[] serialize(Object value) {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(value);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		return bytes.toByteArray();
	}

	record CustomPayload(String name) implements Serializable {

	}

	static final class CustomException extends RuntimeException {

	}

	static final class CustomNumber extends Number {

		@Override
		public int intValue() {
			return 0;
		}

		@Override
		public long longValue() {
			return 0;
		}

		@Override
		public float floatValue() {
			return 0;
		}

		@Override
		public double doubleValue() {
			return 0;
		}

	}

	private static final class TestComponent implements NamedComponent {

		@Override
		public String getComponentName() {
			return "testComponent";
		}

		@Override
		public String getComponentType() {
			return "test";
		}

	}

}
