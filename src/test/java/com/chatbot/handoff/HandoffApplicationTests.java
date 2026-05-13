package com.chatbot.handoff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_AI_API_KEY", matches = ".+")
class HandoffApplicationTests {

	@Test
	void contextLoads() {
	}

}
