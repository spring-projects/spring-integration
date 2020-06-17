/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.ip.udp;

import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;

/**
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class MulticastRule extends TestWatcher {

	public static String GROUP = "225.6.7.8";

	private final String group;

	@Nullable
	private final NetworkInterface nic;

	private boolean skip;

	public MulticastRule() {
		this(GROUP);
	}

	public MulticastRule(String group) {
		Assert.hasText(group, "'group' must not be empty");
		this.group = group;
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("multicast.group", this.group);
		try {
			this.nic = checkMulticast();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		if (this.nic != null) {
			System.setProperty("multicast.local.address", this.nic.getInetAddresses().nextElement().getHostName());
		}
	}

	@Nullable
	private NetworkInterface checkMulticast() throws Exception {
		NetworkInterface nic = SocketTestUtils.chooseANic(true);
		if (nic == null) {    // no multicast support
			this.skip = true;
			return null;
		}
		try {
			MulticastSocket socket = new MulticastSocket();
			socket.joinGroup(new InetSocketAddress(this.group, SocketUtils.findAvailableUdpPort()), nic);
			socket.close();
		}
		catch (Exception e) {
			this.skip = true;
			// Ignore. Assume no Multicast - skip the test.
		}
		return nic;
	}

	public String getGroup() {
		return group;
	}

	@Nullable
	public NetworkInterface getNic() {
		return nic;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		if (this.skip) {
			LogFactory.getLog(getClass()).info("No Multicast support; test skipped");
		}
		Assume.assumeFalse(this.skip);
		return super.apply(base, description);
	}

}
