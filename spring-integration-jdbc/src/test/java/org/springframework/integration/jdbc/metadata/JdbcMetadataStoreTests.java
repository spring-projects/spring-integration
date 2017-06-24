package org.springframework.integration.jdbc.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Bojan Vukasovic
 * @since 5.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext // close at the end after class
@Transactional
public class JdbcMetadataStoreTests {

	@Autowired
	private DataSource dataSource;

	private JdbcMetadataStore metadataStore;

	@Before
	public void init() {
		metadataStore = new JdbcMetadataStore(dataSource);
	}

	@Test
	public void keyAndValuesArePreservedOnPut() {
		metadataStore.put("foo","bar");
		metadataStore.put("foo","bar1");
		metadataStore.put("foo2","bar2");
		String bar1 = metadataStore.get("foo");
		String bar2 = metadataStore.get("foo2");
		assertEquals("bar1", bar1);
		assertEquals("bar2", bar2);
	}

	@Test
	public void keyAndValuesAreNotPreservedOnRemove() {
		metadataStore.put("foo","bar");
		metadataStore.put("foo2","bar2");
		metadataStore.remove("foo");
		String bar = metadataStore.get("foo");
		metadataStore.remove("foo2");
		String bar2 = metadataStore.get("foo2");
		assertNull(bar);
		assertNull(bar2);
	}

	@Test
	public void keyAndValuesAreNotOverwrittenOnPutIfAbsent() {
		metadataStore.put("foo","bar");
		metadataStore.putIfAbsent("foo","bar1");
		String bar = metadataStore.get("foo");
		assertEquals("bar", bar);
	}

	@Test
	public void nonExistentKeyIsNotRemoved() {
		metadataStore.remove("non-existent");
		String ne = metadataStore.get("non-existent");
		assertNull(ne);
	}
}
