/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.smb;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Markus Spann
 * @author Prafull Kumar Soni
 * @author Artem Bilan
 *
 */
public class SmbParserInboundTests extends AbstractBaseTests {

	@BeforeEach
	public void prepare() {
		ensureExists("test-temp/remote-10");
		cleanUp();
	}

	@Test
	@SuppressWarnings("try")
	public void testLocalFilesAutoCreationTrue() {
		assertFileNotExists(new File("test-temp/local-10"));
		try (var ctx = new ClassPathXmlApplicationContext(getApplicationContextXmlFile(), getClass())) {
			assertFileExists(new File("test-temp/local-10"));
			assertFileNotExists(new File("test-temp/local-6"));
		}
	}

	@Test
	public void testLocalFilesAutoCreationFalse() {
		assertFileNotExists(new File("test-temp/local-6"));
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(getApplicationContextXmlFile("-fail"), getClass()));
	}

	@AfterEach
	public void cleanUp() {
		delete("test-temp/local-10", "test-temp/local-6");
	}

}
