package com.chatbot.services;

import java.io.IOException;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatServices {

  public static void main(String[] args) {
    // ConfigurableApplicationContext ctx = SpringApplication.run(ChatServices.class,
    // args);
    SpringApplication.run(ChatServices.class, args);
    // IDMapping map = ctx.getBean(IDMapping.class);
    // BiMap<String, String> bm =
    // map.ChatClientToChatClientBiMapMapping.get(ChatClient.HANGOUTS);
    // System.out.println(bm.toString());
    // DialogflowConversation dc = new DialogflowConversation(System.getenv("projectID"));
    // try {
    //   Value userID = Value.newBuilder().setStringValue("meg").build();
    //   Struct payload = Struct.newBuilder().putFields("userID", userID).build();
    //   System.out.println(dc.sendMessage("I want to change my category to cafe", payload));
    // } catch (IOException e) {
    //   // e.printStackTrace();
    // }
    // try {
    //   System.out.println(dc.getCurrentContexts().toString());
    // } catch (Exception e) {
    //   e.printStackTrace();
    // }
  }
}
