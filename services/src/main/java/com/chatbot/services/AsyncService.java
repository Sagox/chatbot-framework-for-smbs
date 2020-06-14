package com.chatbot.services;

import java.io.IOException;
import java.util.List;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
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

  @Async("asyncExecutor")
  public void hangoutsAsyncHandler(ChatServiceRequest chatServiceRequest) throws Exception {
    String spaceID = chatServiceRequest.getSender().getChatClientGeneratedId();
    switch (chatServiceRequest.getRequestType()) {
      case ADDED:
          hangoutsMessageSender.sendMessage(spaceID, "Thank You for Adding me");
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
          String response = "";
          response = dialogflowConversation
              .sendMessage(chatServiceRequest.getUserMessage().getText(), payload);
          hangoutsMessageSender.sendMessage(spaceID, response);
        } else {
            List<String> currentContextList = dialogflowConversation.getCurrentContexts(); 
            if(currentContextList.contains("ExpectingImagesContext")) {
              // send images to backend
              hangoutsMessageSender.sendMessage(
                  chatServiceRequest.getSender().getChatClientGeneratedId(),
                  "The images have been received!");
            } else {
              hangoutsMessageSender.sendMessage(
                  chatServiceRequest.getSender().getChatClientGeneratedId(),
                  "Sorry, we were not expecting any attachements from you.");
            }
        }
        break;
      default:
        break;
    }
  }

  @Async("asyncExecutor")
  public void fulfillmentAsyncHandler(String userID, String message, ChatClient chatClient) throws IOException {
    if(ChatClient.HANGOUTS.equals(chatClient)) {
      String spaceID = iDMapping.getChatClientGeneratedID(userID, ChatClient.HANGOUTS);
      hangoutsMessageSender.sendMessage(spaceID, message);  
    }
  }

  @Async("asyncExecutor")
  public void whatsappAsyncHandler(ChatServiceRequest chatServiceRequest) {}

}