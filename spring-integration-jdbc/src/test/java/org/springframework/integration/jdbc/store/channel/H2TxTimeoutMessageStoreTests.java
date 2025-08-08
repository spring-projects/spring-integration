/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import org.junit.Rule;

import org.springframework.integration.test.support.LongRunningIntegrationTest;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Manuel Jordan
 * @since 4.3
 */
public class H2TxTimeoutMessageStoreTests extends AbstractTxTimeoutMessageStoreTests {

	@Rule
	public LongRunningIntegrationTest longTests = new LongRunningIntegrationTest();

}
