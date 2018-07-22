package com.TwitterModule;

import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

public class APIKey {
  private String UserName = "";
  private String ConsumerKey = "";
  private String ConsumerSecret = "";
  private String AccessToken = "";
  private String AccessTokenSecret = "";
  private String use = "";
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  APIKey(String APIKeyXmlPath) {
    try {
      openXml(APIKeyXmlPath);
      if((!UserName.equals("")) && (!ConsumerKey.equals("")) && (!ConsumerSecret.equals(""))
        && (!AccessToken.equals("")) && (!AccessTokenSecret.equals("")) && (use.equals("1"))) {
        /*System.out.println("UserName:" + UserName);
        System.out.println("ConsumerKey:" + ConsumerKey);
        System.out.println("ConsumerSecret:" + ConsumerSecret);
        System.out.println("AccessToken:" + AccessToken);
        System.out.println("AccessTokenSecret:" + AccessTokenSecret);
        System.out.println("use:" + use);*/
        System.out.println("APIKey -- OK");
      }else {
        System.out.println("APIKey -- NG");
        System.exit(1);
      }
    }catch (Exception e){
      e.printStackTrace(pw);
      pw.flush();
      System.out.println(StackTrace.toString());
      System.exit(1);
    }
  }
  
  private void openXml(String APIKeyXmlPath) throws Exception {
    DocumentBuilderFactory documentBuilderfactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderfactory.newDocumentBuilder();
    Document document = documentBuilder.parse(new File(APIKeyXmlPath));
    
    Node apiKeyRoot = document.getDocumentElement();
    Node userNode = apiKeyRoot.getFirstChild();
    Node keyNode;
    while(userNode != null) {
      //System.out.println("attribute:" + getAttribute(userNode,"user_name"));
      if(userNode.getNodeType() == Node.ELEMENT_NODE) {
        if(userNode.getNodeName().equals("user")) UserName = getAttribute(userNode,"user_name");
        keyNode = userNode.getFirstChild();
        while(keyNode != null) {
          if(keyNode.getNodeType() == Node.ELEMENT_NODE) {
            switch (keyNode.getNodeName()) {
              case "use":
                use = getTextNode(keyNode.getFirstChild());
                break;
              case "consumer_key":
                ConsumerKey = getTextNode(keyNode.getFirstChild());
                break;
              case "consumer_secret":
                ConsumerSecret = getTextNode(keyNode.getFirstChild());
                break;
              case "access_token":
                AccessToken = getTextNode(keyNode.getFirstChild());
                break;
              case "access_token_secret":
                AccessTokenSecret = getTextNode(keyNode.getFirstChild());
                break;
            }
          }
          keyNode = keyNode.getNextSibling();
        }
      }
      userNode = userNode.getNextSibling();
    }
  }
  
  private String getTextNode(Node node) {
    if(node != null) {
      if(node.getNodeType() == node.TEXT_NODE) return node.getNodeValue();
      else return "";
    }else{
      return "";
    }
  }
  
  private String getAttribute(Node node, String id) {
    NamedNodeMap attributes = node.getAttributes();
    Node attribute;
    if(attributes != null) {
      attribute = attributes.getNamedItem(id);
      if(attribute != null) return attribute.getNodeValue();
      else return "-1"; //missing attributeName
    }else{
      return "0";//not attribute
    }
  }
  
  public String getUserName() {
    return UserName;
  }
  
  public String getConsumerKey() {
    return ConsumerKey;
  }
  
  public String getConsumerSecret() {
    return ConsumerSecret;
  }
  
  public String getAccessToken() {
    return AccessToken;
  }
  
  public String getAccessTokenSecret() {
    return AccessTokenSecret;
  }
}















