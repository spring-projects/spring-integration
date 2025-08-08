/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.outbound;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class BigMGetTests extends org.springframework.integration.file.BigMGetTests {

	@Test
	@Disabled("needs directories and server (FTP)") // TODO use FtpTestSupport
	public void doTest() throws Exception {
		assertThat(this.mgetManyFiles().getPayload().size()).isEqualTo(FILES);
	}

}
