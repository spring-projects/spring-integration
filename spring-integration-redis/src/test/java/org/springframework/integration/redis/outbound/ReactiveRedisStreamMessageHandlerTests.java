package org.springframework.integration.redis.outbound;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Attoumane AHAMADI
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ReactiveRedisStreamMessageHandlerTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	public void emptyStreamKeyTest() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveRedisStreamMessageHandler("", null));
	}

	@Test
	@RedisAvailable
	public void nullConnectionFactoryTest() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ReactiveRedisStreamMessageHandler("stream", null));
	}

}
