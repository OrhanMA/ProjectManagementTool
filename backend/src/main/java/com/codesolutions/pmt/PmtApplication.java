package com.codesolutions.pmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PmtApplication {

  public static void main(String[] args) {
    SpringApplication.run(PmtApplication.class, args);
  }
}
