package com.chatbot.services;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.IllegalCallerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.MimeType;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.RequestType;

@RestController
public class ChatServiceController {

  @Autowired
  private AsyncService asyncService;

  @PostMapping("/")
  public String onRequest(@RequestHeader Map<String, String> headers, @RequestBody JsonNode event) {
    ChatServiceRequest chatServiceRequest;
    String userAgent = headers.get("user-agent");
    if (userAgent.equals("Google-Dynamite")) {
      try {
        chatServiceRequest = parseHangoutsRequest(event); 
        asyncService.hangoutsAsyncHandler(chatServiceRequest);
      } catch (Exception e) {
        // If there was an error in parsing the request, either we do not support the type of
        // request or the format of the request is incorrect, in both these cases returning an empty
        // string is an option.
        e.printStackTrace();
      }
    } else {
        chatServiceRequest = parseWhatsappRequest(event);
        // whatsapp async handler
        // return acknowledgement
    }
      return "";
  }

  private ChatServiceRequest parseHangoutsRequest(JsonNode event) throws Exception {
    ChatServiceRequest.Builder chatServiceRequestBuilder = ChatServiceRequest.newBuilder()
        .setChatClient(ChatClient.HANGOUTS);
    switch (event.at("/type").asText()) {
      case "ADDED_TO_SPACE":
        chatServiceRequestBuilder.setRequestType(RequestType.ADDED);
        String spaceType = event.at("/space/type").asText();
        if ("ROOM".equals(spaceType)) {
          throw new IllegalCallerException("The message was received from a room");
        }
        break;
      case "MESSAGE":
        chatServiceRequestBuilder = parseHangoutsUserMessage(chatServiceRequestBuilder, event);
        break;
      case "REMOVED_FROM_SPACE":
        chatServiceRequestBuilder.setRequestType(RequestType.REMOVED);
        break;
      default:
        throw new IllegalArgumentException("The request has no event type");
    }
    chatServiceRequestBuilder = parseHangoutsSender(chatServiceRequestBuilder, event);
    return chatServiceRequestBuilder.build();
  }

  private ChatServiceRequest.Builder parseHangoutsUserMessage(
      ChatServiceRequest.Builder chatServiceRequestBuilder, JsonNode event) {
    chatServiceRequestBuilder.setRequestType(RequestType.MESSAGE);
    ChatServiceRequest.UserMessage.Builder userMessageBuilder =
        ChatServiceRequest.UserMessage.newBuilder();
    if(event.at("/message").has("attachment")) {
      if(event.at("/message").has("argumentText")) {
        userMessageBuilder.setText(event.at("/message/argumentText").asText());
      }
      Iterator<JsonNode> attachmentIterator = event.at("/message/attachment").elements();
      while(attachmentIterator.hasNext()) {
        JsonNode attachment = (JsonNode)attachmentIterator.next();
        ChatServiceRequest.Attachment.Builder attachmentBuilder =
            ChatServiceRequest.Attachment.newBuilder();
        attachmentBuilder.setLink(attachment.at("/downloadUri").asText());
        switch (attachment.at("/contentType").asText()) {
          case "image/png":
            attachmentBuilder.setMimeType(MimeType.PNG);
            break;
          case "image/jpeg":
            attachmentBuilder.setMimeType(MimeType.JPEG);
            break;
          default:
            attachmentBuilder.setMimeType(MimeType.UNKNOWN_MIME_TYPE);
        }
        userMessageBuilder.addAttachments(attachmentBuilder);
      }
    } else {
      userMessageBuilder.setText(event.at("/message/text").asText());
    }
    chatServiceRequestBuilder.setUserMessage(userMessageBuilder); 
    return chatServiceRequestBuilder;
  }

  private ChatServiceRequest.Builder parseHangoutsSender(
      ChatServiceRequest.Builder chatServiceRequestBuilder, JsonNode event) {
    ChatServiceRequest.Sender.Builder senderBuilder = ChatServiceRequest.Sender.newBuilder();
    senderBuilder.setDisplayName(event.at("/user/displayName").asText())
        .setChatClientGeneratedId(event.at("/space/name").asText().substring(7))
        .setUserId(event.at("/user/name").asText().substring(6));
    chatServiceRequestBuilder.setSender(senderBuilder); 
    return chatServiceRequestBuilder;

  }

  private ChatServiceRequest parseWhatsappRequest(JsonNode event) {
      return null;
  }
}