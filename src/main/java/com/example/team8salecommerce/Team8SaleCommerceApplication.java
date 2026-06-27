package com.example.team8salecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class Team8SaleCommerceApplication {

	public static void main(String[] args) {
		SpringApplication.run(Team8SaleCommerceApplication.class, args);
	}

	@Bean
	public ApplicationRunner updateRoles(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				int updated = jdbcTemplate.update("UPDATE member SET role = 'ADMIN' WHERE role = 'USER'");
				System.out.println(">>> [Antigravity DB Patch] Updated " + updated + " users to ADMIN role.");
			} catch (Exception e) {
				System.err.println(">>> [Antigravity DB Patch] Failed to update roles: " + e.getMessage());
			}
		};
	}
}
