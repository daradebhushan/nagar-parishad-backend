package com.nagar.parishad.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class NagarParishadBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NagarParishadBackendApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner initData(
			com.nagar.parishad.backend.repository.UserRepository userRepository,
			org.springframework.security.crypto.password.PasswordEncoder encoder,
			com.nagar.parishad.backend.service.AuthService authService) {
		return args -> {
			// Seed Owner Account
			authService.seedOwnerAccount();

			String[] emails = { "admin@nagarparishad.in", "owner@govt.in", "ramesh@sanitation.in",
					"head@sanitation.in" };
			for (String email : emails) {
				com.nagar.parishad.backend.entity.User user = userRepository.findByEmail(email).orElse(null);
				if (user != null) {
					user.setPassword(encoder.encode("password"));
					userRepository.save(user);
					System.out.println("DEBUG: Reset Password for " + email);
				}
			}
		};
	}

}
