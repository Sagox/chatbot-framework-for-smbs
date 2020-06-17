package com.chatbot.services;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatServices {

  public static void main(String[] args) throws IOException, InterruptedException, TimeoutException {
    SpringApplication.run(ChatServices.class, args);
  }
}