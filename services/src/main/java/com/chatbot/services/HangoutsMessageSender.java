package com.chatbot.services;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.chat.v1.HangoutsChat;
import com.google.api.services.chat.v1.model.Message;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.stereotype.Component;

// This class initialises the HangoutsChat and can thereafter be used
@Component
public class HangoutsMessageSender {
  
  static final String CHAT_SCOPE = "https://www.googleapis.com/auth/chat.bot";
  private GoogleCredentials credentials;
  private HttpRequestInitializer requestInitializer;
  private HangoutsChat chatService;

  public HangoutsMessageSender() throws GeneralSecurityException, IOException {
    credentials = GoogleCredentials.fromStream(
        HangoutsMessageSender.class.getResourceAsStream("/service-acct.json"))
        .createScoped(CHAT_SCOPE);
    requestInitializer = new HttpCredentialsAdapter(credentials);
    chatService = new HangoutsChat.Builder(
        GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(), requestInitializer)
        .setApplicationName("chatbot").build();
  }

  public void sendMessage(String spaceID, String msg) throws IOException {
    Message message = new Message().setText(msg);
    chatService.spaces().messages().create("spaces/" + spaceID, message).execute();
  }
}