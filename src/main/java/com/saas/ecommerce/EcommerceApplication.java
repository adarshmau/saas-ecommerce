package com.saas.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
public class EcommerceApplication {

	// Ensure JVM timezone is normalized early (applies to tests and main runtime).
	// Some environments set the legacy ID "Asia/Calcutta" as user.timezone which
	// PostgreSQL rejects when the driver sends SET TIME ZONE. Normalize to
	// the canonical IANA name "Asia/Kolkata" here so Flyway / JDBC connections
	// don't fail during application context startup (including tests).
	static {
		// Force the system property so the Postgres driver uses the correct zone
		System.setProperty("user.timezone", "Asia/Kolkata");
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
	}

	public static void main(String[] args) {

        SpringApplication.run(EcommerceApplication.class, args);
	}

}
