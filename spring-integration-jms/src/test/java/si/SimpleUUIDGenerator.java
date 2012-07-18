package si;

import java.util.UUID;

import org.springframework.integration.MessageHeaders.IdGenerator;

public class SimpleUUIDGenerator implements IdGenerator {

	public UUID generateId() {
		return UUID.nameUUIDFromBytes(((System.currentTimeMillis() - System.nanoTime()) + "").getBytes());
	}

}
