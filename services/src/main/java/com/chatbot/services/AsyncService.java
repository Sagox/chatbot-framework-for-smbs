package com.chatbot.services;

import java.io.IOException;
import java.util.List;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

  @Autowired
  private HangoutsMessageSender hangoutsMessageSender;

  @Autowired
  private IDMapping iDMapping;

  private static final String IMAGES_RECEIVED_MESSAGE = "The images have been received!";
  private static final String THANKS_FOR_ADDING_MESSAGE = "Thank You for Adding me";
  private static final String NOT_EXPECTING_IMAGE_MESSAGE =
      "Sorry, we were not expecting any attachements from you.";
  private static final String EXPECTING_IMAGES_CONTEXT = "ExpectingImagesContext";
  
  @Async("asyncExecutor")
  public void hangoutsAsyncHandler(ChatServiceRequest chatServiceRequest) throws Exception {
    String spaceID = chatServiceRequest.getSender().getChatClientGeneratedId();
    switch (chatServiceRequest.getRequestType()) {
      case ADDED:
          hangoutsMessageSender.sendMessage(spaceID, THANKS_FOR_ADDING_MESSAGE);
          iDMapping.addNewMapping(chatServiceRequest.getSender().getChatClientGeneratedId(),
              chatServiceRequest.getSender().getUserId(), chatServiceRequest.getChatClient());
        break;
      case REMOVED:
        break;
      case MESSAGE:
        // The spaceID of the user is used as the sessionID for hangouts
        DialogflowConversation dialogflowConversation =
            new DialogflowConversation(System.getenv("projectID"), spaceID);
        if (chatServiceRequest.getUserMessage().getAttachmentsCount() == 0) {
          Value userID = Value.newBuilder()
              .setStringValue(chatServiceRequest.getSender().getUserId()).build();
          Struct payload = Struct.newBuilder().putFields("userID", userID).build();
          String response = dialogflowConversation
              .sendMessage(chatServiceRequest.getUserMessage().getText(), payload);
          hangoutsMessageSender.sendMessage(spaceID, response);
        } else {
            List<String> currentContextList = dialogflowConversation.getCurrentContexts(); 
            if(currentContextList.contains(EXPECTING_IMAGES_CONTEXT)) {
              // send images to backend
              hangoutsMessageSender.sendMessage(
                  chatServiceRequest.getSender().getChatClientGeneratedId(),
                  IMAGES_RECEIVED_MESSAGE);
            } else {
              hangoutsMessageSender.sendMessage(
                  chatServiceRequest.getSender().getChatClientGeneratedId(),
                  NOT_EXPECTING_IMAGE_MESSAGE);
            }
        }
        break;
      default:
        break;
    }
  }

  @Async("asyncExecutor")
  public void sendMessageUsingUserID(String userID, String message, ChatClient chatClient)
      throws IOException {
    switch(chatClient) {
      case HANGOUTS:
        hangoutsMessageSender.sendMessage(
            iDMapping.getChatClientGeneratedID(userID, ChatClient.HANGOUTS), message);
        break;
      case WHATSAPP:
        hangoutsMessageSender.sendMessage(
            iDMapping.getChatClientGeneratedID(userID, ChatClient.WHATSAPP), message);
        break;
      default:
        throw new IllegalArgumentException("Unknown chat client found");
    }
  }

  public void sendMessageUsingChatClientGeneratedID(String chatClientGeneratedID, String message,
      ChatClient chatClient) throws IOException {
    switch(chatClient) {
      case HANGOUTS:
        hangoutsMessageSender.sendMessage(chatClientGeneratedID, message);
        break;
      case WHATSAPP:
        // send whatsapp message
      default:
        throw new IllegalArgumentException("Unknown chat client found");
    }
  }

  public void triggerEventHandler(TriggerEventNotification triggerEventNotification) throws IOException {
    ChatClient chatClient = ChatClient.valueOf(triggerEventNotification.getChatClient().name());
    String userID = triggerEventNotification.getUserID();
    String chatClientGeneratedID = iDMapping.getChatClientGeneratedID(userID, chatClient);
    DialogflowConversation dialogflowConversation = 
        new DialogflowConversation(System.getenv("projectID"), chatClientGeneratedID);
    Value userIDValue = Value.newBuilder().setStringValue(userID).build();
    Struct payload = Struct.newBuilder().putFields("userID", userIDValue).build();
    String triggerResponse = dialogflowConversation.triggerEvent(
        triggerEventNotification.getEvent().name(), triggerEventNotification.getEventParams(), payload);
    sendMessageUsingChatClientGeneratedID(chatClientGeneratedID, triggerResponse, chatClient);
  }
  @Async("asyncExecutor")
  public void whatsappAsyncHandler(ChatServiceRequest chatServiceRequest) {}

}