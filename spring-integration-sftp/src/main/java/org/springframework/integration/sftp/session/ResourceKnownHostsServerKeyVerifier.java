/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.integration.sftp.session;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.net.SshdSocketAddress;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A {@link ServerKeyVerifier} implementation for a {@link Resource} abstraction.
 * The logic is similar to the {@link KnownHostsServerKeyVerifier}, but in read-only mode.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see KnownHostsServerKeyVerifier
 */
public class ResourceKnownHostsServerKeyVerifier implements ServerKeyVerifier {

	private static final Log logger = LogFactory.getLog(ResourceKnownHostsServerKeyVerifier.class);

	private final Supplier<Collection<KnownHostsServerKeyVerifier.HostEntryPair>> keysSupplier;

	public ResourceKnownHostsServerKeyVerifier(Resource knownHostsResource) {
		Assert.notNull(knownHostsResource, "'knownHostsResource' must not be null");
		this.keysSupplier = GenericUtils.memoizeLock(getKnownHostSupplier(knownHostsResource));
	}

	@Override
	public boolean verifyServerKey(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey) {
		Collection<KnownHostsServerKeyVerifier.HostEntryPair> knownHosts = this.keysSupplier.get();
		List<KnownHostsServerKeyVerifier.HostEntryPair> matches =
				findKnownHostEntries(clientSession, remoteAddress, knownHosts);

		if (matches.isEmpty()) {
			return false;
		}

		String serverKeyType = KeyUtils.getKeyType(serverKey);

		return matches.stream()
				.filter(match -> serverKeyType.equals(match.getHostEntry().getKeyEntry().getKeyType()))
				.filter(match -> KeyUtils.compareKeys(match.getServerKey(), serverKey))
				.anyMatch(match -> !"revoked".equals(match.getHostEntry().getMarker()));
	}

	private static Supplier<Collection<KnownHostsServerKeyVerifier.HostEntryPair>> getKnownHostSupplier(
			Resource knownHostsResource) {

		return () -> {
			try {
				Collection<KnownHostEntry> entries =
						KnownHostEntry.readKnownHostEntries(knownHostsResource.getInputStream(), true);
				List<KnownHostsServerKeyVerifier.HostEntryPair> keys = new ArrayList<>(entries.size());
				for (KnownHostEntry entry : entries) {
					keys.add(new KnownHostsServerKeyVerifier.HostEntryPair(entry, resolveHostKey(entry)));
				}
				return keys;
			}
			catch (Exception ex) {
				logger.warn("Known hosts cannot be loaded from the: " + knownHostsResource, ex);
				return Collections.emptyList();
			}
		};
	}

	private static PublicKey resolveHostKey(KnownHostEntry entry) throws IOException, GeneralSecurityException {
		AuthorizedKeyEntry authEntry = entry.getKeyEntry();
		Assert.notNull(authEntry, () -> "No key extracted from " + entry);
		return authEntry.resolvePublicKey(null, PublicKeyEntryResolver.IGNORING);
	}

	private static List<KnownHostsServerKeyVerifier.HostEntryPair> findKnownHostEntries(
			ClientSession clientSession, SocketAddress remoteAddress,
			Collection<KnownHostsServerKeyVerifier.HostEntryPair> knownHosts) {

		if (GenericUtils.isEmpty(knownHosts)) {
			return Collections.emptyList();
		}

		Collection<SshdSocketAddress> candidates = resolveHostNetworkIdentities(clientSession, remoteAddress);

		if (GenericUtils.isEmpty(candidates)) {
			return Collections.emptyList();
		}

		List<KnownHostsServerKeyVerifier.HostEntryPair> matches = new ArrayList<>();
		for (KnownHostsServerKeyVerifier.HostEntryPair match : knownHosts) {
			KnownHostEntry entry = match.getHostEntry();
			for (SshdSocketAddress host : candidates) {
				if (entry.isHostMatch(host.getHostName(), host.getPort())) {
					matches.add(match);
					break;
				}
			}
		}

		return matches;
	}

	private static Collection<SshdSocketAddress> resolveHostNetworkIdentities(
			ClientSession clientSession, SocketAddress remoteAddress) {
		/*
		 * NOTE !!! we do not resolve the fully-qualified name to avoid long DNS timeouts.
		 * Instead, we use the reported peer address and the original connection target host
		 */
		Collection<SshdSocketAddress> candidates = new TreeSet<>(SshdSocketAddress.BY_HOST_AND_PORT);
		candidates.add(SshdSocketAddress.toSshdSocketAddress(remoteAddress));
		SocketAddress connectAddress = clientSession.getConnectAddress();
		candidates.add(SshdSocketAddress.toSshdSocketAddress(connectAddress));
		return candidates;
	}

}
