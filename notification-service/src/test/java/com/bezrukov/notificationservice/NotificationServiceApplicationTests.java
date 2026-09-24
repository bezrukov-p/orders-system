package com.bezrukov.notificationservice;

import com.bezrukov.notificationservice.integrations.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
