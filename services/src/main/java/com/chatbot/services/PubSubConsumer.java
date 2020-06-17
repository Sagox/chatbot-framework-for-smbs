package com.chatbot.services;

import java.io.IOException;
import java.util.Map;

import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.ChatClient;
import com.chatbot.services.protobuf.TriggerEventNotificationOuterClass.TriggerEventNotification.Event;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.Struct;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PubSubConsumer {

  @Autowired
  private AsyncService asyncService;
  private static String projectID;
  private static String subscriptionID;
  private static final String TRIGGER_EVENT_MESSAGE = "TriggerEvent";
  private static final String SUGGEST_CATEGORY_CHANGE_EVENT = "SUGGEST_CATEGORY_CHANGE";
  private static final String SUGGEST_IMAGE_UPLOAD_EVENT = "SUGGEST_IMAGE_UPLOAD";
  private static Long maxOutstandingElements;
  private static Long maxOutstandingBytes;

  public PubSubConsumer(
      @Value("${pubsubConfig.maxOutstandingElements}") String maxOutstandingElementsToSet,
      @Value("${pubsubConfig.maxOutstandingBytes}") String maxOutstandingBytesToSet)
      throws InterruptedException {
    projectID = System.getenv("projectID");
    subscriptionID = System.getenv("subscriptionID");
    maxOutstandingElements = Long.parseLong(maxOutstandingElementsToSet);
    maxOutstandingBytes = Long.parseLong(maxOutstandingBytesToSet);
    launchSubscriber();
  }

  private void launchSubscriber()
      throws InterruptedException, IllegalArgumentException {
    ProjectSubscriptionName subscriptionName = ProjectSubscriptionName
        .of(projectID, subscriptionID);
    MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
      String messageData = message.getData().toStringUtf8();
      Map<String, String> messageAttributesMap = message.getAttributesMap();
      if(messageData.equals(TRIGGER_EVENT_MESSAGE)) {
        TriggerEventNotification triggerEventNotification = 
            buildNotificationFromMessage(messageAttributesMap);
        try {
          asyncService.triggerEventHandler(triggerEventNotification);
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        throw new IllegalArgumentException("Unknown message received at subscriber");
      }
      consumer.ack();
    };
    FlowControlSettings flowControlSettings = FlowControlSettings.newBuilder()
        .setMaxOutstandingElementCount(maxOutstandingElements)
        .setMaxOutstandingRequestBytes(maxOutstandingBytes)
        .build();
    ExecutorProvider executorProvider = InstantiatingExecutorProvider.newBuilder()
        .setExecutorThreadCount(4)
        .build();
    Subscriber subscriber = Subscriber.newBuilder(subscriptionName, receiver)
        .setFlowControlSettings(flowControlSettings)
        .setExecutorProvider(executorProvider)
        .build();
    subscriber.startAsync().awaitRunning();
  }

  private TriggerEventNotification buildNotificationFromMessage(
      Map<String, String> messageAttributesMap) throws IllegalArgumentException {
    TriggerEventNotification.Builder triggerEventNotificationBuilder = 
        TriggerEventNotification.newBuilder();
    if(messageAttributesMap.containsKey("userID")) {
      triggerEventNotificationBuilder.setUserID(messageAttributesMap.get("userID"));
    } else {
      throw new IllegalArgumentException("No userID provided in published message");
    }
    if(messageAttributesMap.containsKey("chatClient")) {
      String chatClient = messageAttributesMap.get("chatClient");
      switch (chatClient) {
        case "HANGOUTS":
          triggerEventNotificationBuilder.setChatClient(ChatClient.HANGOUTS);
          break;
        case "WHATSAPP":
          triggerEventNotificationBuilder.setChatClient(ChatClient.WHATSAPP);
          break;
        default:
          throw new IllegalArgumentException("Unknown client provided in published message"); 
      }
    } else {
      throw new IllegalArgumentException("No chat client provided in published message");
    } 
    if(messageAttributesMap.containsKey("event")) {
      String event = messageAttributesMap.get("event");
      switch (event) {
        case SUGGEST_CATEGORY_CHANGE_EVENT:
          com.google.protobuf.Value suggestedCategory = com.google.protobuf.Value.newBuilder()
          .setStringValue(messageAttributesMap.get("suggestedCategory")).build();
          Struct eventParams = Struct.newBuilder().putFields("suggestedCategory", suggestedCategory).build();
          triggerEventNotificationBuilder
              .setEvent(Event.SUGGEST_CATEGORY_CHANGE).setEventParams(eventParams);
          break;
        case SUGGEST_IMAGE_UPLOAD_EVENT:
          triggerEventNotificationBuilder.setEvent(Event.SUGGEST_IMAGE_UPLOAD);
          break;
        default:
          throw new IllegalArgumentException("Unknown event provided in published message");
      }
    } else {
      throw new IllegalArgumentException("No event provided in published message");
    }    
    return triggerEventNotificationBuilder.build();
  }
}