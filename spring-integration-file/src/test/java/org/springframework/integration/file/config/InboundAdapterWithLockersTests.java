/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class InboundAdapterWithLockersTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testAdaptersWithLockers() {
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerA"),
				"source.scanner.locker")).isEqualTo(context.getBean("locker"));
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerB"),
				"source.scanner.locker")).isEqualTo(context.getBean("locker"));
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerC"),
				"source.scanner.locker")).isInstanceOf(NioFileLocker.class);
		assertThat(TestUtils.<Object>getPropertyValue(context.getBean("inputWithLockerD"),
				"source.scanner.locker")).isInstanceOf(NioFileLocker.class);
	}

}
