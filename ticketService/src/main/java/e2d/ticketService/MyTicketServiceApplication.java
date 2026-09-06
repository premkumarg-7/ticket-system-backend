package e2d.ticketService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MyTicketServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyTicketServiceApplication.class, args);
	}

}
