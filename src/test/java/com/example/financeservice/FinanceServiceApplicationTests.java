package com.example.financeservice;

import com.example.financeservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class FinanceServiceApplicationTests {

	@MockBean
	private UserService userService;

	@Test
	void contextLoads() {
		try {
			System.out.println("Context loaded successfully.");
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
