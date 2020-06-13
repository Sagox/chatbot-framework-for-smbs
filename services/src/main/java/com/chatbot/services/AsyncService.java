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

  // public void AsyncService() {
  // System.out.println("\n\n\nAsync Service Constructor\n\n\n");
  // }

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
        DialogflowConversation dialogflowConversation = new DialogflowConversation(System.getenv("projectID"), spaceID);
        if (chatServiceRequest.getUserMessage().getAttachmentsCount() == 0) {
          Value userID = Value.newBuilder().setStringValue(chatServiceRequest.getSender().getUserId()).build();
          Struct payload = Struct.newBuilder().putFields("userID", userID).build();
          String response = "";
          response = dialogflowConversation.sendMessage(chatServiceRequest.getUserMessage().getText(), payload);
            // System.out.println("Detect query failed for sessionID: " + spaceID);
            hangoutsMessageSender.sendMessage(spaceID, response);
            // System.out.println("Could not send hangouts message to user with spaceID: " + spaceID);
        } else {
            List<String> currentContextList = dialogflowConversation.getCurrentContexts(); 
            System.out.println(currentContextList.toString());
            if(currentContextList.contains("ExpectingImagesContext")) {
              // send images to backend
              hangoutsMessageSender.sendMessage(chatServiceRequest.getSender().getChatClientGeneratedId(),
              "The images have been received!");
            } else {
              hangoutsMessageSender.sendMessage(chatServiceRequest.getSender().getChatClientGeneratedId(),
              "Sorry, we were not expecting any attachements from you.");
            }
        }
        break;
      default:
        break;
    }
  }

  @Async("asyncExecutor")
  public void fulfillmentAsyncHandler(String userID, String message) throws IOException {
    String spaceID = iDMapping.getChatClientGeneratedID(userID, ChatClient.HANGOUTS);
    hangoutsMessageSender.sendMessage(spaceID, message);
  }

  @Async("asyncExecutor")
  public void whatsappAsyncHandler(ChatServiceRequest chatServiceRequest) {}

}