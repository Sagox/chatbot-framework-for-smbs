package com.chatbot.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.chatbot.services.protobuf.ChatServiceRequestOuterClass.ChatServiceRequest.ChatClient;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DialogflowFulfillmentController {

  @Autowired
  private AsyncService asyncService;

  @PostMapping("/dgf")
  public String onEvent(@RequestHeader final Map<String, String> headers,
      @RequestBody final JsonNode event) throws JsonParseException, IOException {
    String intentName = event.at("/queryResult/intent/displayName").asText();
    String userID = event.at("/originalDetectIntentRequest/payload/userID").asText();
    JsonNode parameters = event.at("/queryResult/outputContexts").get(0).at("/parameters");
    Map<String, String> parameterMap = new HashMap<>();
    Iterator<String> paramKeyIterator = parameters.fieldNames();
    while(paramKeyIterator.hasNext()) {
      String paramName = (String) paramKeyIterator.next();
      parameterMap.put(paramName, parameters.get(paramName).asText());
    }
    switch(intentName) {
      case "ChangeCategory":
        // change the category to paramMap.get("suggestedCategory") for userID
        asyncService.sendMessageUsingUserID(userID,
            buildCategoryChangedMessage(parameterMap.get("suggestedCategory")),
            ChatClient.HANGOUTS);
    }
    // an empty string is returned so that the responses added to the dialogflow console are
    // used
    return "";
  }

  private String buildCategoryChangedMessage(String suggestedCategory) {
    return "Your category has been changed to " + suggestedCategory;
  }
}