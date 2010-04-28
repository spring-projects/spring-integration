package org.springframework.integration.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;

/**
 * @author Dave Syer
 *
 */
public class UUIDConverterTests {

	@Test
	public void testConvertNull() throws Exception {
		assertNull(UUIDConverter.getUUID(null));
	}

	@Test
	public void testConvertUUID() throws Exception {
		UUID uuid = UUID.randomUUID();
		assertEquals(uuid, UUIDConverter.getUUID(uuid));
	}

	@Test
	public void testConvertUUIDString() throws Exception {
		UUID uuid = UUID.randomUUID();
		assertEquals(uuid, UUIDConverter.getUUID(uuid.toString()));
	}

	@Test
	public void testConvertAlmostUUIDString() throws Exception {
		String name = "1-2-3-4";
		try {
			UUID.fromString(name);
			fail();
		} catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: "+message, message.contains("Invalid UUID string"));
		}
		assertNotNull(UUIDConverter.getUUID(name));
	}

	@Test
	public void testConvertRandomString() throws Exception {
		assertNotNull(UUIDConverter.getUUID("foo"));
	}

	@Test
	public void testConvertPrimitive() throws Exception {
		assertNotNull(UUIDConverter.getUUID(1L));
	}

	@Test
	public void testConvertSerializable() throws Exception {
		assertNotNull(UUIDConverter.getUUID(new Date()));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConvertNonSerializable() throws Exception {
		assertNotNull(UUIDConverter.getUUID(new Object()));
	}

}
