/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import org.junit.Rule;

import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
@ContextConfiguration
public class HsqlTxTimeoutMessageStoreTests extends AbstractTxTimeoutMessageStoreTests {

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

}
