/*
 * Copyright 2025-present the original author or authors.
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

import java.lang.reflect.AnnotatedElement;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.util.Assert;

/**
 * A JUnit condition that checks whether the system supports multicast or not.
 * If it is not supported, tests will be skipped.
 * <p>
 * The default multicast group is "225.6.7.8", but a custom group can be specified.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 6.5.0
 */
public class MulticastCondition implements BeforeAllCallback, ParameterResolver {

	public static final String DEFAULT_GROUP = "225.6.7.8";

	private static final int PROBE_TIMEOUT = 5000;

	private String group;

	private NetworkInterface nic;

	private boolean skip;

	public void checkMulticast(String group) {
		Assert.hasText(group, "'group' must not be empty");
		this.group = group;
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("multicast.group", group);
		try {
			this.nic = doCheckMulticast(group);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		if (this.nic != null) {
			System.setProperty("multicast.local.address", this.nic.getInetAddresses().nextElement().getHostName());
		}
	}

	private NetworkInterface doCheckMulticast(String group) throws Exception {
		NetworkInterface nic = SocketTestUtils.chooseANic(true);
		if (nic == null) {    // no multicast support
			this.skip = true;
			return null;
		}
		if (!multicastRoutes(group, nic)) {
			this.skip = true;
			// Ignore. Assume no Multicast - skip the test.
		}
		return nic;
	}

	/**
	 * Verify that a multicast datagram really does reach a member of the group.
	 * Joining a group can succeed on hosts which nevertheless drop every multicast
	 * datagram - some CI images behave this way - and then the tests fail instead of
	 * being skipped. Therefore, this probe performs the same round trip as the tests
	 * do: send from a socket bound to the site-local address of the {@code nic} and
	 * receive on a {@link MulticastSocket} joined to the group over that same
	 * {@code nic}.
	 * @param group the multicast group to probe.
	 * @param nic the network interface to probe the group over.
	 * @return true if the datagram sent to the group is received back.
	 */
	private boolean multicastRoutes(String group, NetworkInterface nic) {
		InetAddress siteLocalAddress = siteLocalAddress(nic);
		if (siteLocalAddress == null) {
			return false;
		}
		byte[] probe = ("multicast-probe-" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
		try (MulticastSocket receiver = new MulticastSocket()) {
			receiver.setNetworkInterface(nic);
			receiver.setSoTimeout(PROBE_TIMEOUT);
			InetAddress groupAddress = InetAddress.getByName(group);
			InetSocketAddress groupSocketAddress = new InetSocketAddress(groupAddress, 0);
			receiver.joinGroup(groupSocketAddress, nic);
			try (DatagramSocket sender = new DatagramSocket(0, siteLocalAddress)) {
				sender.send(new DatagramPacket(probe, probe.length,
						new InetSocketAddress(groupAddress, receiver.getLocalPort())));
			}
			DatagramPacket received = new DatagramPacket(new byte[probe.length], probe.length);
			receiver.receive(received);
			receiver.leaveGroup(groupSocketAddress, nic);
			return Arrays.equals(probe, Arrays.copyOf(received.getData(), received.getLength()));
		}
		catch (Exception ex) {
			return false;
		}
	}

	private static InetAddress siteLocalAddress(NetworkInterface nic) {
		Enumeration<InetAddress> addresses = nic.getInetAddresses();
		while (addresses.hasMoreElements()) {
			InetAddress address = addresses.nextElement();
			if (address.isSiteLocalAddress()
					&& !address.isAnyLocalAddress()
					&& !address.isLinkLocalAddress()
					&& !address.isLoopbackAddress()) {

				return address;
			}
		}
		return null;
	}

	public String getGroup() {
		return group;
	}

	public NetworkInterface getNic() {
		return nic;
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		Optional<AnnotatedElement> element = context.getElement();
		MergedAnnotations annotations = MergedAnnotations.from(element.get(),
				MergedAnnotations.SearchStrategy.DIRECT);
		MergedAnnotation<Multicast> mergedAnnotation = annotations.get(Multicast.class);
		String group = DEFAULT_GROUP;
		if (mergedAnnotation.isPresent()) {
			Multicast multicast = mergedAnnotation.synthesize();
			group = multicast.group();
		}
		checkMulticast(group);

		if (this.skip) {
			LogFactory.getLog(getClass()).info("No Multicast support; test skipped");
		}
		Assumptions.assumeFalse(this.skip);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.getParameter().getType() == MulticastCondition.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return this;
	}

}
