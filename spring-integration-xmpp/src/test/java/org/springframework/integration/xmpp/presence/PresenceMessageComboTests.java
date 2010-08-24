/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.xmpp.presence;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This class will demonstrate using both inbound adapter types in a 1-2 punch of:
 * <UL>
 * <LI> notifying the bus of a user's sudden online availability using &lt;xmpp:roster-event-inbound-channel-adapter&gt;</LI>
 * <LI> sending that user a message using the &lt;xmpp:outbound-message-channel-adapter /&gt;</LI>
 * </UL>
 *
 * @author Josh Long
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PresenceMessageComboTests {

	@Test
	@Ignore
	public void run() throws Throwable {
		Thread.sleep(60 * 1000);
	}

}
