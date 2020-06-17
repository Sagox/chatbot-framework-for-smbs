package com.chatbot.services;

import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
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

  private static final String HANGOUTS_USER_AGENT = "Google-Dynamite";
  private static final String REMOVED_FROM_SPACE_EVENT = "REMOVED_FROM_SPACE";
  private static final String ADDED_TO_SPACE_EVENT = "ADDED_TO_SPACE";
  private static final String MESSAGE_EVENT = "MESSAGE";

  @PostMapping("/")
  public String onRequest(@RequestHeader Map<String, String> headers, @RequestBody JsonNode event) {
    String userAgent = headers.get("user-agent");
    if (userAgent.equals(HANGOUTS_USER_AGENT)) {
      try {
        asyncService.hangoutsAsyncHandler(parseHangoutsRequest(event));
      } catch (Exception e) {
        // If there was an error in parsing the request, either we do not support the type of
        // request or the format of the request is incorrect, in both these cases returning an empty
        // string is an option.
        e.printStackTrace();
      }
    } else {
        // parseWhatsappRequest(event);
        // whatsapp async handler
        // return acknowledgement
    }
      return "";
  }

  private ChatServiceRequest parseHangoutsRequest(JsonNode event) throws Exception {
    if ("ROOM".equals(event.at("/space/type").asText())) {
      throw new IllegalArgumentException("The message was received from a room");
    }
    ChatServiceRequest.Builder chatServiceRequestBuilder = ChatServiceRequest.newBuilder()
        .setChatClient(ChatClient.HANGOUTS);
    switch (event.at("/type").asText()) {
      case ADDED_TO_SPACE_EVENT:
        chatServiceRequestBuilder.setRequestType(RequestType.ADDED);
        break;
      case MESSAGE_EVENT:
        chatServiceRequestBuilder.setRequestType(RequestType.MESSAGE);
        chatServiceRequestBuilder = parseHangoutsUserMessage(chatServiceRequestBuilder, event);
        break;
      case REMOVED_FROM_SPACE_EVENT:
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
}