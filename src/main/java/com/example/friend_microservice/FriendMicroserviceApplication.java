package com.example.friend_microservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.friend_microservice", "com.example"})
public class FriendMicroserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FriendMicroserviceApplication.class, args);
	}

}
