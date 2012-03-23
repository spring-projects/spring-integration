/**
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.smb;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;

/**
 * @author Markus Spann
 *
 */
public class SmbMessageHistoryTests extends AbstractBaseTest {
	@Test
	public void testMessageHistory() throws Exception {
		SourcePollingChannelAdapter adapter = getApplicationContext().getBean("smbInboundChannelAdapter", SourcePollingChannelAdapter.class);
		assertEquals("smbInboundChannelAdapter", adapter.getComponentName());
		assertEquals("smb:inbound-channel-adapter", adapter.getComponentType());
	}
}
