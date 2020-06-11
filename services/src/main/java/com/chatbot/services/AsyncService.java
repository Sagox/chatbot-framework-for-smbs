package com.chatbot.services;

import java.io.IOException;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

  @Autowired
  private HangoutsMessageSender hangoutsMessageSender;

  @Async("asyncExecutor")
  public void hangoutsAsyncHandler(ChatServiceRequest chatServiceRequest) throws IOException {
    switch (chatServiceRequest.getRequestType()) {
      case ADDED:
        hangoutsMessageSender.sendMessage(chatServiceRequest.getSender().getChatClientGeneratedId(),
            "Thank You for Adding me");
      break;
      case REMOVED:
        break;
      case MESSAGE:
        if(chatServiceRequest.getUserMessage().getAttachmentsCount() == 0) {
          Value userID = Value.newBuilder().setStringValue(chatServiceRequest.getSender()
              .getUserId()).build();
          Struct payload = Struct.newBuilder().putFields("userID", userID).build();
          DialogflowConversation dialogflowConversation = new DialogflowConversation(System
              .getenv("projectID"), chatServiceRequest.getSender().getChatClientGeneratedId());
          String response = dialogflowConversation.sendMessage(
              chatServiceRequest.getUserMessage().getText(), payload);
          hangoutsMessageSender.sendMessage(chatServiceRequest.getSender()
              .getChatClientGeneratedId(),response);
        } else {
          // fetch current contexts for user
          // based on contexts send the attachments to backend
        }
        break;
      default:
        break;
    }
  }

  @Async("asyncExecutor")
  public void whatsappAsyncHandler(ChatServiceRequest chatServiceRequest) {}

}