package com.chatbot.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

import javax.websocket.RemoteEndpoint.Async;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.IllegalCallerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.RequestType;
import com.chatbot.services.AsyncService;

@RestController
public class ChatServiceController {

  @Autowired
  private AsyncService asyncService;

  @PostMapping("/")
  public String onEvent(@RequestHeader Map<String, String> headers, @RequestBody JsonNode event)
       throws IOException, GeneralSecurityException, InterruptedException {
      ChatServiceRequest chatServiceRequest;
      String userAgent = headers.get("user-agent");
      if(userAgent.equals("Google-Dynamite")) {
        chatServiceRequest = parseHangoutsRequest(event);
      } else {
        chatServiceRequest = parseWhatsappRequest(event);
      }
      if(chatServiceRequest.getChatClient() == ChatServiceRequest.ChatClient.HANGOUTS) {
        asyncService.hangoutsAsyncHandler(chatServiceRequest);
        return "";
      } else {
        // whatsapp async handler
        // return acknowledgement
      }
      return "";
  }

  public ChatServiceRequest parseHangoutsRequest(JsonNode event) {
    ChatServiceRequest.Builder chatServiceRequestBuilder = ChatServiceRequest.newBuilder();
    chatServiceRequestBuilder.setChatClient(ChatClient.HANGOUTS);
    switch (event.at("/type").asText()) {
      case "ADDED_TO_SPACE":
        chatServiceRequestBuilder.setRequestType(RequestType.ADDED);
        String spaceType = event.at("/space/type").asText();
        if ("ROOM".equals(spaceType)) {
          throw new IllegalCallerException("The message was received from a room");
        }
        break;
      case "MESSAGE":
        chatServiceRequestBuilder.setRequestType(RequestType.MESSAGE);
        ChatServiceRequest.UserMessage.Builder userMessageBuilder =
            ChatServiceRequest.UserMessage.newBuilder()
            .setText(event.at("/message/text").asText());
        chatServiceRequestBuilder.setUserMessage(userMessageBuilder); 
        break;
      case "REMOVED_FROM_SPACE":
        chatServiceRequestBuilder.setRequestType(RequestType.REMOVED);
        break;
      default:
        throw new IllegalArgumentException("The request has no event type");
    }
    ChatServiceRequest.Sender.Builder senderBuilder =
    ChatServiceRequest.Sender.newBuilder();
    senderBuilder.setDisplayName(event.at("/user/displayName").asText())
        .setChatClientGeneratedId(event.at("/space/type").asText().substring(7))
        .setUserId(event.at("/user/displayName").asText().substring(6));
    chatServiceRequestBuilder.setSender(senderBuilder); 
    return chatServiceRequestBuilder.build();
  }

  public ChatServiceRequest parseWhatsappRequest(JsonNode event) {
      return null;
  }
}