/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ftp;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpMessageHistoryTests {

	@Autowired
	SourcePollingChannelAdapter adapter;

	@Test
	public void testMessageHistory() {
		assertThat(adapter.getComponentName()).isEqualTo("adapterFtp");
		assertThat(adapter.getComponentType()).isEqualTo("ftp:inbound-channel-adapter");
	}

}
