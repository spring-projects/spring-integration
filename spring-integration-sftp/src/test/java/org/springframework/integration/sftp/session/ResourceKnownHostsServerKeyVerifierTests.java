/*
 * Copyright 2022-2024 the original author or authors.
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
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.sshd.client.ClientFactoryManager;
import org.apache.sshd.client.config.hosts.HostPatternsHolder;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.config.keys.AuthorizedKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.random.JceRandomFactory;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.Mockito;

import org.springframework.core.io.FileSystemResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
@EnabledIf("isDefaultKnownHostsFilePresent")
public class ResourceKnownHostsServerKeyVerifierTests {

	private static final String HASHED_HOST = "192.168.1.61";

	private static final Map<SshdSocketAddress, PublicKey> HOST_KEYS = new TreeMap<>(SshdSocketAddress.BY_HOST_AND_PORT);

	@BeforeAll
	static void loadHostKeys() throws GeneralSecurityException, IOException {
		Map<SshdSocketAddress, KnownHostEntry> hostsEntries = loadEntries(KnownHostEntry.getDefaultKnownHostsFile());
		for (Map.Entry<SshdSocketAddress, KnownHostEntry> ke : hostsEntries.entrySet()) {
			SshdSocketAddress hostIdentity = ke.getKey();
			KnownHostEntry entry = ke.getValue();
			AuthorizedKeyEntry authEntry = entry.getKeyEntry();
			PublicKey key = authEntry.resolvePublicKey(null, Collections.emptyMap(), PublicKeyEntryResolver.FAILING);
			HOST_KEYS.put(hostIdentity, key);
		}
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void testServerKeys() {
		ResourceKnownHostsServerKeyVerifier verifier
				= new ResourceKnownHostsServerKeyVerifier(
				new FileSystemResource(KnownHostEntry.getDefaultKnownHostsFile()));

		ClientFactoryManager manager = mock();
		Mockito.when(manager.getRandomFactory()).thenReturn((Factory) JceRandomFactory.INSTANCE);

		HOST_KEYS.forEach((key, value) -> {
			ClientSession session = mock();
			Mockito.when(session.getFactoryManager()).thenReturn(manager);

			Mockito.when(session.getConnectAddress()).thenReturn(key);
			assertThat(verifier.verifyServerKey(session, key, value)).isTrue();
		});
	}

	private static Map<SshdSocketAddress, KnownHostEntry> loadEntries(Path file) throws IOException {
		Collection<KnownHostEntry> entries = KnownHostEntry.readKnownHostEntries(file);
		if (GenericUtils.isEmpty(entries)) {
			return Collections.emptyMap();
		}

		Map<SshdSocketAddress, KnownHostEntry> hostsMap = new TreeMap<>(SshdSocketAddress.BY_HOST_AND_PORT);
		for (KnownHostEntry entry : entries) {
			String line = entry.getConfigLine();
			// extract hosts
			int pos = line.indexOf(' ');
			String patterns = line.substring(0, pos);
			if (entry.getHashedEntry() != null) {
				hostsMap.put(new SshdSocketAddress(HASHED_HOST, 0), entry);
			}
			else {
				String[] addrs = GenericUtils.split(patterns, ',');
				for (String a : addrs) {
					int port = 0;
					if (a.charAt(0) == HostPatternsHolder.NON_STANDARD_PORT_PATTERN_ENCLOSURE_START_DELIM) {
						pos = a.indexOf(HostPatternsHolder.NON_STANDARD_PORT_PATTERN_ENCLOSURE_END_DELIM, 1);

						port = Integer.parseInt(a.substring(pos + 2));
						a = a.substring(1, pos);
					}
					hostsMap.put(new SshdSocketAddress(a, port), entry);
				}
			}
		}

		return hostsMap;
	}

	static boolean isDefaultKnownHostsFilePresent() {
		return KnownHostEntry.getDefaultKnownHostsFile().toFile().exists();
	}

}
