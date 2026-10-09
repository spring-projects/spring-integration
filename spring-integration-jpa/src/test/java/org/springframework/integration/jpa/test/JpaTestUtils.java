/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.test.entity.Gender;
import org.springframework.integration.jpa.test.entity.StudentDomain;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
public final class JpaTestUtils {

	private JpaTestUtils() {
		super();
	}

	public static StudentDomain getTestStudent() {

		return new StudentDomain()
				.withFirstName("First Executor")
				.withLastName("Last Executor")
				.withGender(Gender.MALE)
				.withDateOfBirth(LocalDate.of(1984, 1, 31))
				.withLastUpdated(LocalDateTime.now());
	}

	public static SourcePollingChannelAdapter getSourcePollingChannelAdapter(MessageSource<?> adapter,
			MessageChannel channel,
			PollerMetadata poller,
			GenericApplicationContext context,
			ClassLoader beanClassLoader) {

		SourcePollingChannelAdapterFactoryBean fb = new SourcePollingChannelAdapterFactoryBean();
		fb.setSource(adapter);
		fb.setOutputChannel(channel);
		fb.setPollerMetadata(poller);
		fb.setBeanClassLoader(beanClassLoader);
		fb.setAutoStartup(false);
		fb.setBeanFactory(context.getBeanFactory());
		fb.afterPropertiesSet();

		return fb.getObject();
	}

}
