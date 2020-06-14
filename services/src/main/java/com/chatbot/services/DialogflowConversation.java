package com.chatbot.services;

import com.google.cloud.dialogflow.v2.Context;
import com.google.cloud.dialogflow.v2.ContextsClient;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.QueryParameters;
import com.google.cloud.dialogflow.v2.QueryResult;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.Struct;
import com.google.cloud.dialogflow.v2.EventInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DialogflowConversation {

  String projectID;
  String langCode;
  String sessionID;

  public DialogflowConversation(String projectID, String langCode, String sessionID) {
    this.projectID = projectID;
    this.langCode = langCode;
    this.sessionID = sessionID;
  }

  public DialogflowConversation(String projectID, String sessionID) {
    this.projectID = projectID;
    this.langCode = "en";
    this.sessionID = sessionID;
  }

  public DialogflowConversation(String projectID) {
    this.projectID = projectID;
    this.langCode = "en";
    // if the session ID is not provided, generate a random UUID
    this.sessionID = UUID.randomUUID().toString();
  }
  // function to send a message to dialogflow and get the response
  public String sendMessage(String message, Struct payload) throws IOException {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      SessionName session = SessionName.of(this.projectID, this.sessionID);
      TextInput.Builder textInput = TextInput.newBuilder()
          .setText(message)
          .setLanguageCode(this.langCode);
      // the query to be sent to dialogflow
      QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();
      // build the query params
      QueryParameters queryParameters = QueryParameters.newBuilder().setPayload(payload).build();
      // Performs the detect intent request
      DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
          .setSession(session.toString())
          .setQueryInput(queryInput)
          .setQueryParams(queryParameters)
          .build();
      DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
      QueryResult queryResult = response.getQueryResult();
      return queryResult.getFulfillmentText();
    }
  }
  // function to trigger a dialogflow event and get the reponse
  public String triggerEvent(String event, Struct parameters, Struct payload) throws IOException {
    try (SessionsClient sessionsClient = SessionsClient.create()) {
      SessionName session = SessionName.of(this.projectID, this.sessionID);
      // the event to be triggered at dialogflow
      EventInput.Builder eventInput = EventInput.newBuilder().setName(event)
          .setParameters(parameters).setLanguageCode(this.langCode);
      // set the query to the event to be triggered
      QueryInput queryInput = QueryInput.newBuilder().setEvent(eventInput).build();
      // build the query params
      QueryParameters queryParameters = QueryParameters.newBuilder().setPayload(payload)
        .build();
      // Performs the detect intent request
      DetectIntentRequest detectIntentRequest = DetectIntentRequest.newBuilder()
          .setSession(session.toString())
          .setQueryInput(queryInput)
          .setQueryParams(queryParameters)
          .build();
      DetectIntentResponse response = sessionsClient.detectIntent(detectIntentRequest);
      QueryResult queryResult = response.getQueryResult();
      return queryResult.getFulfillmentText();
    }
  }

  public List<String> getCurrentContexts() throws Exception {
    List<String> contextList = new ArrayList<String>();
    try (ContextsClient contextsClient = ContextsClient.create()) {
      // Performs the list contexts request
      SessionName session = SessionName.of(this.projectID, this.sessionID);
      for (Context context : contextsClient.listContexts(session).iterateAll()) {
        contextList.add(context.getName());
      }
    }
    return contextList;
  }
}