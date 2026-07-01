package gr.pants.tdebt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TdebtApplication {

	public static void main(String[] args) {
		SpringApplication.run(TdebtApplication.class, args);
	}

}
