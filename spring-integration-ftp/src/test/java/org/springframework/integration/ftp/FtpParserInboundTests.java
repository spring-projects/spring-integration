/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class FtpParserInboundTests {

	@BeforeEach
	public void prepare() {
		new File("target/foo").delete();
	}

	@Test
	public void testLocalFilesAutoCreationTrue() {
		assertThat(new File("target/foo").exists()).isFalse();
		new ClassPathXmlApplicationContext("FtpParserInboundTests-context.xml", this.getClass()).close();
		assertThat(new File("target/foo").exists()).isTrue();
		assertThat(new File("target/bar").exists()).isFalse();
	}

	@Test
	public void testLocalFilesAutoCreationFalse() {
		assertThat(new File("target/bar").exists()).isFalse();

		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("FtpParserInboundTests-fail-context.xml", this.getClass()))
				.withCauseInstanceOf(BeanInitializationException.class)
				.withRootCauseInstanceOf(FileNotFoundException.class)
				.withStackTraceContaining("bar");
	}

	@AfterEach
	public void cleanUp() {
		new File("target/foo").delete();
	}

}
