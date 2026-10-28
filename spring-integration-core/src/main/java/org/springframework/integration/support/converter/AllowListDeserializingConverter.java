/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.support.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * A {@link Converter} that delegates to a
 * {@link Deserializer} to convert data in a byte
 * array to an object. By default, only the classes listed in {@link #DEFAULT_ALLOWED_PATTERNS}
 * (plus primitives, {@link String} and arrays of any of these) are deserialized.
 * Use {@link #addAllowedPatterns(String...)} to trust additional classes/packages,
 * or {@link #setAllowedPatterns(String...)} to replace the defaults.
 * The {@code "*"} pattern allows all classes.
 * <p>
 * If a delegate deserializer is a {@link DefaultDeserializer}, only its {@link ClassLoader}
 * is used for a {@link ConfigurableObjectInputStream} logic.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.4
 */
public class AllowListDeserializingConverter implements Converter<byte[], Object> {

	/**
	 * The classes allowed for deserialization by default.
	 * @since 7.2
	 */
	public static final List<String> DEFAULT_ALLOWED_PATTERNS =
			List.of(
					// Messaging
					"org.springframework.messaging.MessageHeaders",
					"org.springframework.messaging.support.GenericMessage",
					"org.springframework.messaging.support.ErrorMessage",
					"org.springframework.util.MimeType",
					"org.springframework.core.NestedRuntimeException",
					"org.springframework.messaging.MessagingException",
					"org.springframework.messaging.MessageDeliveryException",
					"org.springframework.messaging.MessageHandlingException",
					// Spring Integration
					"org.springframework.integration.MessageRejectedException",
					"org.springframework.integration.MessageTimeoutException",
					"org.springframework.integration.MessageDispatchingException",
					"org.springframework.integration.support.MutableMessage",
					"org.springframework.integration.support.MutableMessageHeaders",
					"org.springframework.integration.message.AdviceMessage",
					"org.springframework.integration.history.MessageHistory",
					"org.springframework.integration.history.MessageHistory$Entry",
					"org.springframework.integration.store.MessageHolder",
					"org.springframework.integration.store.MessageMetadata",
					"org.springframework.integration.store.MessageGroupMetadata",
					"org.springframework.integration.handler.DelayHandler$DelayedMessageWrapper",
					"org.springframework.integration.file.splitter.FileSplitter$FileMarker",
					"org.springframework.integration.file.splitter.FileSplitter$FileMarker$Mark",
					// java.util
					"java.util.HashMap",
					"java.util.LinkedHashMap",
					"java.util.TreeMap",
					"java.util.Hashtable",
					"java.util.Properties",
					"java.util.ArrayList",
					"java.util.LinkedList",
					"java.util.HashSet",
					"java.util.LinkedHashSet",
					"java.util.TreeSet",
					"java.util.UUID",
					"java.util.Date",
					"java.util.Arrays$ArrayList",
					"java.util.CollSer",
					"java.util.Collections$SingletonList",
					"java.util.Collections$SingletonSet",
					"java.util.Collections$SingletonMap",
					"java.util.Collections$EmptyList",
					"java.util.Collections$EmptySet",
					"java.util.Collections$EmptyMap",
					"java.util.Collections$UnmodifiableCollection",
					"java.util.Collections$UnmodifiableList",
					"java.util.Collections$UnmodifiableRandomAccessList",
					"java.util.Collections$UnmodifiableSet",
					"java.util.Collections$UnmodifiableSortedSet",
					"java.util.Collections$UnmodifiableNavigableSet",
					"java.util.Collections$UnmodifiableMap",
					"java.util.Collections$UnmodifiableSortedMap",
					"java.util.Collections$UnmodifiableNavigableMap",
					"java.util.NoSuchElementException",
					"java.util.concurrent.TimeoutException",
					// java.lang
					"java.lang.Object",
					"java.lang.Boolean",
					"java.lang.Character",
					"java.lang.Enum",
					"java.lang.StackTraceElement",
					"java.lang.Throwable",
					"java.lang.Exception",
					"java.lang.RuntimeException",
					"java.lang.IllegalStateException",
					"java.lang.IllegalArgumentException",
					"java.lang.NullPointerException",
					"java.lang.UnsupportedOperationException",
					"java.lang.ClassCastException",
					"java.lang.IndexOutOfBoundsException",
					// Numbers
					"java.lang.Number",
					"java.lang.Byte",
					"java.lang.Short",
					"java.lang.Integer",
					"java.lang.Long",
					"java.lang.Float",
					"java.lang.Double",
					"java.math.BigInteger",
					"java.math.BigDecimal",
					// java.io
					"java.io.File",
					"java.io.IOException",
					"java.io.UncheckedIOException",
					// java.time
					"java.time.Instant",
					"java.time.Duration",
					"java.time.Ser"
			);

	private final Deserializer<Object> deserializer;

	private final @Nullable ClassLoader defaultDeserializerClassLoader;

	private final boolean usingDefaultDeserializer;

	private final Set<String> allowedPatterns = new LinkedHashSet<>(DEFAULT_ALLOWED_PATTERNS);

	/**
	 * Create a {@link AllowListDeserializingConverter} with default
	 * {@link ObjectInputStream} configuration, using the "latest user-defined
	 * ClassLoader".
	 */
	public AllowListDeserializingConverter() {
		this(new DefaultDeserializer());
	}

	/**
	 * Create a {@link AllowListDeserializingConverter} for using an
	 * {@link ObjectInputStream} with the given {@code ClassLoader}.
	 * @param classLoader the class loader to use for deserialization.
	 */
	public AllowListDeserializingConverter(ClassLoader classLoader) {
		this(new DefaultDeserializer(classLoader));
	}

	/**
	 * Create a {@link AllowListDeserializingConverter} that delegates to the provided
	 * {@link Deserializer}.
	 * @param deserializer the deserializer to use.
	 */
	public AllowListDeserializingConverter(Deserializer<Object> deserializer) {
		Assert.notNull(deserializer, "Deserializer must not be null");
		this.deserializer = deserializer;
		if (deserializer instanceof DefaultDeserializer defaultDeserializer) {
			this.defaultDeserializerClassLoader = defaultDeserializer.getClassLoader();
			this.usingDefaultDeserializer = true;
		}
		else {
			this.defaultDeserializerClassLoader = null;
			this.usingDefaultDeserializer = false;
		}
	}

	/**
	 * Set simple patterns for allowable packages/classes for deserialization.
	 * The patterns will be applied in order until a match is found.
	 * A class can be fully qualified, or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.example.*}, {@code *.MyClass}.
	 * Replaces all the existing patterns, including the {@link #DEFAULT_ALLOWED_PATTERNS}.
	 * Use {@link #addAllowedPatterns(String...)} to keep the defaults.
	 * @param allowedPatterns the patterns; must not be empty.
	 */
	public void setAllowedPatterns(String... allowedPatterns) {
		assertPatterns(allowedPatterns);
		Assert.notEmpty(allowedPatterns, "'allowedPatterns' must not be empty");
		this.allowedPatterns.clear();
		Collections.addAll(this.allowedPatterns, allowedPatterns);
	}

	/**
	 * Add package/class patterns to the allowed list, in addition to the existing ones
	 * (by default the {@link #DEFAULT_ALLOWED_PATTERNS}).
	 * @param patterns the patterns to add.
	 * @see #setAllowedPatterns(String...)
	 */
	public void addAllowedPatterns(String... patterns) {
		assertPatterns(patterns);
		Collections.addAll(this.allowedPatterns, patterns);
	}

	private static void assertPatterns(String... patterns) {
		Assert.notNull(patterns, "'patterns' must not be null");
		for (String pattern : patterns) {
			Assert.hasText(pattern, "'patterns' must not contain null, empty or blank elements");
		}
	}

	/**
	 * Return the current allowed patterns.
	 * @return an unmodifiable view of the allowed patterns.
	 * @since 7.2
	 */
	public Set<String> getAllowedPatterns() {
		return Collections.unmodifiableSet(this.allowedPatterns);
	}

	@Override
	public Object convert(byte[] source) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
		try {
			if (this.usingDefaultDeserializer) {
				return deserialize(byteStream);
			}

			Object result = this.deserializer.deserialize(byteStream);
			/* Even if there is no knowledge what is the target deserialization algorithm
			 and malicious code may be executed already, it is still better to fail
			 with untrusted data rather than just let it pass downstream.
			 */
			checkAllowList(result.getClass());
			return result;
		}
		catch (Exception ex) {
			throw new SerializationFailedException("Failed to deserialize payload. " +
					"Is the byte array a result of corresponding serialization for " +
					this.deserializer.getClass().getSimpleName() + "?", ex);
		}
	}

	protected Object deserialize(ByteArrayInputStream inputStream) throws IOException {
		try {
			ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream,
					this.defaultDeserializerClassLoader) {

				@Override
				protected Class<?> resolveClass(ObjectStreamClass classDesc)
						throws IOException, ClassNotFoundException {

					Class<?> clazz = super.resolveClass(classDesc);
					checkAllowList(clazz);
					return clazz;
				}

			};
			return objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex) {
			throw new IOException("Failed to deserialize object type", ex);
		}
	}

	protected void checkAllowList(Class<?> clazz) {
		Class<?> typeToCheck = clazz;
		while (typeToCheck.isArray()) {
			typeToCheck = typeToCheck.getComponentType();
		}
		if (typeToCheck.isPrimitive() || typeToCheck.equals(String.class)) {
			return;
		}
		String className = typeToCheck.getName();
		for (String pattern : this.allowedPatterns) {
			if (PatternMatchUtils.simpleMatch(pattern, className)) {
				return;
			}
		}
		throw new SecurityException("Attempt to deserialize unauthorized " + clazz +
				"; add its package/class via addAllowedPatterns()");
	}

}
