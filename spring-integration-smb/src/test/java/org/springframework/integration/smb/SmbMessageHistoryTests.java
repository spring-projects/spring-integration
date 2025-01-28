/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.smb;

import java.net.URI;
import java.net.URL;
import java.util.Properties;

import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
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

			CIFSContext context = new BaseContext(new PropertyConfiguration(new Properties()));
			URL rawUrl = new URL(null, smbSessionFactory.rawUrl(true), context.getUrlHandler());
			assertThat(rawUrl.getHost()).isEqualTo("localhost");
			assertThat(rawUrl.getUserInfo()).isEqualTo("sambagu%40est:sambag%25uest");
			assertThat(rawUrl.getPath()).isEqualTo("/smb share/");
		}
	}

}
