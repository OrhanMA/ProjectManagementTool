package com.codesolutions.pmt;

import org.springframework.boot.SpringApplication;

public class TestPmtApplication {

  public static void main(String[] args) {
    SpringApplication.from(PmtApplication::main).with(TestcontainersConfiguration.class).run(args);
  }
}
