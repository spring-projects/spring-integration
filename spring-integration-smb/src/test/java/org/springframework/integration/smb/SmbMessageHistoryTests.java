/*
 * Copyright 2012-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.smb.session.SmbSessionFactory;

/**
 * @author Markus Spann
 * @author Prafull Kumar Soni
 * @author Artem Bilan
 * @author Gregory Bragg
 */
public class SmbMessageHistoryTests extends AbstractBaseTests {

	@Test
	public void testMessageHistory() throws URISyntaxException {
		ClassPathXmlApplicationContext applicationContext = getApplicationContext();
		SourcePollingChannelAdapter adapter = applicationContext
				.getBean("smbInboundChannelAdapter", SourcePollingChannelAdapter.class);
		assertThat("smbInboundChannelAdapter").isEqualTo(adapter.getComponentName());
		assertThat("smb:inbound-channel-adapter").isEqualTo(adapter.getComponentType());

		SmbSessionFactory smbSessionFactory = applicationContext.getBean(SmbSessionFactory.class);

		String url = smbSessionFactory.getUrl();
		URI uri = new URI(url);
		assertThat("sambagu%40est:sambag%25uest").isEqualTo(uri.getRawUserInfo());
		assertThat("sambagu@est:sambag%uest").isEqualTo(uri.getUserInfo());

		applicationContext.close();
	}
}
