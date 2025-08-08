/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.test;

import java.util.Calendar;
import java.util.Date;

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
 * @since 2.2
 *
 */
public final class JpaTestUtils {

	private JpaTestUtils() {
		super();
	}

	public static StudentDomain getTestStudent() {

		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1984, 0, 31);

		StudentDomain student = new StudentDomain()
				.withFirstName("First Executor")
				.withLastName("Last Executor")
				.withGender(Gender.MALE)
				.withDateOfBirth(dateOfBirth.getTime())
				.withLastUpdated(new Date());

		return student;
	}

	public static SourcePollingChannelAdapter getSourcePollingChannelAdapter(MessageSource<?> adapter,
			MessageChannel channel,
			PollerMetadata poller,
			GenericApplicationContext context,
			ClassLoader beanClassLoader) throws Exception {

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
