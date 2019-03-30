package com.TwitterModule;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

public class APIKey {
  private String UserName = "";
  private String ConsumerKey = "";
  private String ConsumerSecret = "";
  private String AccessToken = "";
  private String AccessTokenSecret = "";
  private String UserNo = "";
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  APIKey(String APIKeyXmlPath) {
    try {
      File existAPIKeyXml;
      existAPIKeyXml = new File(APIKeyXmlPath);
      if(existAPIKeyXml.exists() == false) APIKeyError("APIKey.xml の読み込みに失敗しました.");
      openAPIKeyXml(APIKeyXmlPath);
      if((!UserName.equals("")) && (!ConsumerKey.equals("")) && (!ConsumerSecret.equals(""))
        && (!AccessToken.equals("")) && (!AccessTokenSecret.equals("")) && (UserNo.equals("1"))) {
        /*System.out.println("UserName:" + UserName);
        System.out.println("ConsumerKey:" + ConsumerKey);
        System.out.println("ConsumerSecret:" + ConsumerSecret);
        System.out.println("AccessToken:" + AccessToken);
        System.out.println("AccessTokenSecret:" + AccessTokenSecret);
        System.out.println("Use:" + UserNo);*/
        System.out.println("APIKey.xml -- OK");
      }else {
        APIKeyError("Error:APIKey.xml のキー情報の参照に失敗しました.");
      }
    }catch (Exception e){
      e.printStackTrace(pw);
      pw.flush();
      System.out.println(StackTrace.toString());
      System.exit(1);
    }
  }
  
  private void openAPIKeyXml(String APIKeyXmlPath) throws Exception {
    DocumentBuilderFactory documentBuilderfactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderfactory.newDocumentBuilder();
    Document document = documentBuilder.parse(new File(APIKeyXmlPath));
    //int userCount = 0;
    Node apiKeyRoot = document.getDocumentElement();
    Node childNode = apiKeyRoot.getFirstChild();
    //Node consumerNode,userNode;
    Map<String,String> consumerMap,userMap;
    
    while(childNode != null) {
      //System.out.println(childNode.getNodeName());
      if(childNode.getNodeType() == Node.ELEMENT_NODE) {
        if(childNode.getNodeName().equals("consumer")) {
          consumerMap = getNodeItem(childNode.getFirstChild());
          ConsumerKey = consumerMap.get("consumer_key");
          ConsumerSecret = consumerMap.get("consumer_secret");
          if((ConsumerKey == null) || (ConsumerSecret == null)) APIKeyError("Error:APIKey.xml の構文に誤りがあります."); 
        }else if(childNode.getNodeName().equals("user")){
          UserName = getAttribute(childNode,"user_name");
          userMap = getNodeItem(childNode.getFirstChild());
          UserNo = userMap.get("user_no");
          AccessToken = userMap.get("access_token");
          AccessTokenSecret = userMap.get("access_token_secret");
          if((UserNo == null) || (AccessToken == null) || (AccessTokenSecret == null)) APIKeyError("Error:APIKey.xml の構文に誤りがあります.");
        }
      }
      childNode = childNode.getNextSibling();
    }
  }
  
  private void APIKeyError(String msg) {
    System.out.println(msg);
    System.exit(1);
  }
  
  private Map<String, String> getNodeItem(Node node) {
    Map<String,String> nodeItem = new HashMap<String,String>();
    while(node != null) {
      //System.out.println("  " + node.getNodeName());
      if(node.getNodeType() == Node.ELEMENT_NODE) nodeItem.put(node.getNodeName(), getTextNode(node.getFirstChild()));
      node = node.getNextSibling();
    }
    return nodeItem;
  }
  
  private String getTextNode(Node node) {
    if(node != null) {
      //System.out.println("    " + node.getNodeValue());
      if(node.getNodeType() == Node.TEXT_NODE) return node.getNodeValue();
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















