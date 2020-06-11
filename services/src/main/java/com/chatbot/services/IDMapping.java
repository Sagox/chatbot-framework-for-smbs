package com.chatbot.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.chat.v1.HangoutsChat;
import com.google.api.services.chat.v1.model.Membership;
import com.google.api.services.chat.v1.model.Space;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.springframework.stereotype.Component;

@Component
public class IDMapping {
  enum ChatClient {
    UNKNOWN,
    WHATSAPP,
    HANGOUTS
  };
  
  Map<ChatClient, BiMap<String, String>> ChatClientToChatClientBiMapMapping;
  public IDMapping() throws GeneralSecurityException, IOException {
    ChatClientToChatClientBiMapMapping = new HashMap<>();
    ChatClientToChatClientBiMapMapping.put(ChatClient.WHATSAPP, HashBiMap.create());
    ChatClientToChatClientBiMapMapping.put(ChatClient.HANGOUTS, HashBiMap.create());
    populateHangoutsBiMap();
  }

  // this function populates the hangouts spaceID to userID mapping
  void populateHangoutsBiMap() throws GeneralSecurityException, IOException {
    final String CHAT_SCOPE = "https://www.googleapis.com/auth/chat.bot";
    GoogleCredentials credentials = GoogleCredentials
        .fromStream(IDMapping.class.getResourceAsStream("/service-acct.json"))
        .createScoped(CHAT_SCOPE);
    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    HangoutsChat chatService;
    chatService = new HangoutsChat.Builder(GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(), requestInitializer)
            .setApplicationName("basic-async-bot-java").build();
    List<Space> spacesList = chatService.spaces().list().execute().getSpaces();
    for(Space space: spacesList) {
      String spaceName = space.getName();
      List<Membership> memebershipList =chatService.spaces().members().list(spaceName).execute()
            .getMemberships();
      for(Membership membership: memebershipList) {
        ChatClientToChatClientBiMapMapping.get(ChatClient.HANGOUTS)
            .put(spaceName.substring(7), membership.getMember().getName().substring(6));
      }
    }
  }
  // this function populates the whatsappID to userID mapping
  void populateWhatsappBiMap() {}

  // function to get the chat client specific ID for a user given the userID and the chat client
  public String getChatClientGeneratedID(String userID, ChatClient chatClient) {
    return ChatClientToChatClientBiMapMapping.get(chatClient).inverse().get(userID);
  }

  // function to get the user ID associated with a given chat client generated ID
  public String getUserID(String chatClientGeneratedID) {
    for(ChatClient chatClient: ChatClient.values()) {
      if(ChatClientToChatClientBiMapMapping.containsKey(chatClient)) {
        if(ChatClientToChatClientBiMapMapping.get(chatClient).containsKey(chatClientGeneratedID)) {
          return ChatClientToChatClientBiMapMapping.get(chatClient).get(chatClientGeneratedID);
        }
      }
    }
    return "";
  }

  // function to get the user ID associated with a given chat client generated ID and the chat
  // client
  public String getUserID(String chatClientGeneratedID, ChatClient chatClient) {
    if(ChatClientToChatClientBiMapMapping.get(chatClient).containsKey(chatClientGeneratedID)) {
      return ChatClientToChatClientBiMapMapping.get(chatClient).get(chatClientGeneratedID);
    }
    return "";
  }
}