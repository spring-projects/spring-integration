/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.smb;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.smb.session.SmbSessionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Markus Spann
 * @author Prafull Kumar Soni
 * @author Artem Bilan
 * @author Gregory Bragg
 * @author Jelle Smits
 */
public class SmbMessageHistoryTests extends AbstractBaseTests {

	@Test
	public void testMessageHistory() throws Exception {
		try (ClassPathXmlApplicationContext applicationContext = getApplicationContext()) {
			SourcePollingChannelAdapter adapter = applicationContext
					.getBean("smbInboundChannelAdapter", SourcePollingChannelAdapter.class);
			assertThat("smbInboundChannelAdapter").isEqualTo(adapter.getComponentName());
			assertThat("smb:inbound-channel-adapter").isEqualTo(adapter.getComponentType());

			SmbSessionFactory smbSessionFactory = applicationContext.getBean(SmbSessionFactory.class);

			String url = smbSessionFactory.getUrl();
			URI uri = new URI(url);
			assertThat(uri.getRawUserInfo()).isEqualTo("sambagu%40est:sambag%25uest");
			assertThat(uri.getUserInfo()).isEqualTo("sambagu@est:sambag%uest");
			assertThat(uri.getPath()).isEqualTo("/smb share/");
			assertThat(uri.getRawPath()).isEqualTo("/smb%20share/");
		}
	}

}
