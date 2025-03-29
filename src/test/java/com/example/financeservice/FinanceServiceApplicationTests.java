package com.example.financeservice;

import com.example.financeservice.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FinanceServiceApplicationTests {

	@MockBean
	private UserService userService;

	@Test
	void contextLoads() {
	}

}
