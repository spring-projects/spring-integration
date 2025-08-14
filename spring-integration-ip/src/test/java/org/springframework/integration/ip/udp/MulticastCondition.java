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
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Optional;

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
 *
 * @since 6.5.0
 */
public class MulticastCondition implements BeforeAllCallback, ParameterResolver {

	public static final String DEFAULT_GROUP = "225.6.7.8";

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
		try {
			MulticastSocket socket = new MulticastSocket();
			socket.joinGroup(new InetSocketAddress(group, 0), nic);
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
