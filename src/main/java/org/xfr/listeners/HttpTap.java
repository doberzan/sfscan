package org.xfr.listeners;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.params.HttpParameterType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xfr.SFScan;

import java.net.URI;
import java.net.URLDecoder;

import java.nio.charset.StandardCharsets;
import java.util.*;


import static burp.api.montoya.http.handler.RequestToBeSentAction.continueWith;
import static burp.api.montoya.http.handler.ResponseReceivedAction.continueWith;

public class HttpTap implements HttpHandler {
    private final SFScan sfScan;
    private final ObjectMapper mapper;
    public String target;
    public String currentContext;
    public HttpTap(MontoyaApi api, SFScan sfScan) {
        this.mapper = new ObjectMapper();
        this.sfScan = sfScan;
        this.target = "";
        this.currentContext = "";
        sfScan.log = api.logging();
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {

        // Collect message body for SF POST messages
        if(requestToBeSent.method().equalsIgnoreCase("POST") && requestToBeSent.hasParameter("message", HttpParameterType.BODY))
        {
            String message = URLDecoder.decode(requestToBeSent.parameterValue("message", HttpParameterType.BODY), StandardCharsets.UTF_8);
            if(currentContext == null ||currentContext.isEmpty())
            {
                currentContext = requestToBeSent.parameterValue("aura.context", HttpParameterType.BODY);
            }
            if(target == null||target.isEmpty())
            {
                target = URI.create(requestToBeSent.url()).getHost();
                sfScan.ui.objectsTab.targetURL.setText(target);
                sfScan.ui.objectsTab.sendObjectsQuery();
            }
            try {
                JsonNode baseObject = mapper.readTree(message);
                for (JsonNode action : baseObject.get("actions")) {
                    String methodName = parseSFAction(action);
                    if(!sfScan.ui.methodsTab.endPoints.containsKey(methodName) && methodName != null) {
                        sfScan.ui.methodsTab.endPoints.put(methodName, new TreeSet<>());
                        sfScan.ui.methodsTab.updateMethods();
                        sfScan.ui.methodsTab.exampleRequestForEndpoints.put(methodName, requestToBeSent.toString());
                    }
                    updateSFParameters(action, sfScan.ui.methodsTab.endPoints.get(methodName));
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }
        return continueWith(requestToBeSent);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {

        return continueWith(responseReceived);
    }

    private String parseSFAction(JsonNode action)
    {
        if(action.has("params") && action.get("params").has("classname") && action.get("params").has("method"))
        {
            return action.get("params").get("classname").textValue().strip() + "." + action.get("params").get("method").textValue().strip();
        }
        return null;
    }

    private void updateSFParameters(JsonNode action, TreeSet<String> params) {
        if(action.has("params") && action.get("params").has("classname") && action.get("params").has("params"))
        {
            Iterator<String> it = action.get("params").get("params").fieldNames();
            int failSafe = 100;
            while(it.hasNext() && failSafe > 0)
            {
                failSafe -= 1;
                String tmp = it.next();
                if(tmp != null) {
                    params.add(tmp);
                }
            }
        }
    }
}