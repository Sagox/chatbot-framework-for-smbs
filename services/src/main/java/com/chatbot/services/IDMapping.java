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
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IDMapping {


  private String CHAT_SCOPE;
  private Map<ChatClient, BiMap<String, String>> ChatClientToChatClientBiMapMapping;

  public IDMapping(@Value("${hangoutsAPIScope}") String apiScope) throws GeneralSecurityException,
      IOException {
    this.CHAT_SCOPE = apiScope;
    ChatClientToChatClientBiMapMapping = new HashMap<ChatClient, BiMap<String, String>>();
    ChatClientToChatClientBiMapMapping.put(ChatClient.WHATSAPP, HashBiMap.create(100));
    ChatClientToChatClientBiMapMapping.put(ChatClient.HANGOUTS, HashBiMap.create(100));
    populateHangoutsBiMap();
  }

  private void populateHangoutsBiMap() throws GeneralSecurityException, IOException {
    GoogleCredentials credentials = GoogleCredentials
        .fromStream(IDMapping.class.getResourceAsStream("/service-acct.json"))
        .createScoped(CHAT_SCOPE);
    HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
    HangoutsChat chatService;
    chatService = new HangoutsChat.Builder(GoogleNetHttpTransport.newTrustedTransport(),
        JacksonFactory.getDefaultInstance(), requestInitializer)
            .setApplicationName("chatbot").build();
    List<Space> spacesList = chatService.spaces().list().execute().getSpaces();
    for(Space space: spacesList) {
      String spaceName = space.getName();
      List<Membership> memebershipList = chatService.spaces().members().list(spaceName).execute()
          .getMemberships();
      for(Membership membership: memebershipList) {
        ChatClientToChatClientBiMapMapping.get(ChatClient.HANGOUTS)
            .put(spaceName.substring(7), membership.getMember().getName().substring(6));
      }
    }
  }

  public void addNewMapping(String chatClientGeneratedID, String userID, ChatClient chatClient) {
    ChatClientToChatClientBiMapMapping.get(chatClient).put(chatClientGeneratedID, userID);
  }

  // get the chat client specific ID for a user given the userID and the chat client
  public String getChatClientGeneratedID(String userID, ChatClient chatClient) {
    return ChatClientToChatClientBiMapMapping.get(chatClient).inverse().get(userID);
  }

  // get the chat client specific ID for a user given the userID
  public String getChatClientGeneratedID(String userID) {
    for(ChatClient chatClient: ChatClient.values()) {
      if(ChatClientToChatClientBiMapMapping.containsKey(chatClient)) {
        if(ChatClientToChatClientBiMapMapping.get(chatClient).inverse().containsKey(userID)) {
          return ChatClientToChatClientBiMapMapping.get(chatClient).inverse().get(userID);
        }
      }
    }
    return "";
  }

  // get the user ID associated with a given chat client generated ID
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

  // get the user ID associated with a given chat client generated ID and the chat client
  public String getUserID(String chatClientGeneratedID, ChatClient chatClient) {
    if(ChatClientToChatClientBiMapMapping.get(chatClient).containsKey(chatClientGeneratedID)) {
      return ChatClientToChatClientBiMapMapping.get(chatClient).get(chatClientGeneratedID);
    }
    return "";
  }
}