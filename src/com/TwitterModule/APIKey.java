package com.TwitterModule;

import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class APIKey {
  private String XMLPath = "";
  private Document document;
  private String UserName = "";
  private String ConsumerKey = "";
  private String ConsumerSecret = "";
  private String AccessToken = "";
  private String AccessTokenSecret = "";
  private String UserNo = "";
  private String ErrorCheckMsg = "";
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  public APIKey(String APIKeyXmlPath) {
    try {
      XMLPath = APIKeyXmlPath;
      String tempFilePath = deleteIndentXML();
      File tempFile = new File(tempFilePath);
      DocumentBuilderFactory documentBuilderfactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderfactory.newDocumentBuilder();
      document = documentBuilder.parse(tempFile);
      tempFile.delete();
      readConsumerKey();
      System.out.println("Info:APIKey.xmlの読み込み -- OK");
      
    }catch (Exception e){
      e.printStackTrace(pw);
      pw.flush();
      APIKeyError(StackTrace.toString());
    }
  }
  
  private void readConsumerKey() {
    try {
      Node apiKeyRoot = document.getDocumentElement();
      Node childNode = apiKeyRoot.getFirstChild();
      Map<String, String> consumerMap;
      while(childNode != null) {
        if((childNode.getNodeType() == Node.ELEMENT_NODE) && (childNode.getNodeName().equals("consumer"))) {
          consumerMap = getNodeItem(childNode.getFirstChild());
          ConsumerKey = consumerMap.get("consumer_key");
          ConsumerSecret = consumerMap.get("consumer_secret");
          if((ConsumerKey == null) || (ConsumerSecret == null)) APIKeyError("Error:APIKey.xml の構文に誤りがあります."); 
          if(errorCheck(0)) APIKeyError(ErrorCheckMsg);
        }
        childNode = childNode.getNextSibling();
      }
    }catch (Exception e) {
      e.printStackTrace(pw);
      pw.flush();
      APIKeyError(StackTrace.toString());
    }
  }
  
  private boolean readAccessToken(String SetUserName) {
    boolean existUserCheck = false;
    try {
      Node apiKeyRoot = document.getDocumentElement();
      Node childNode = apiKeyRoot.getFirstChild();
      Map<String,String> userMap;
      
      while(childNode != null) {
        if((childNode.getNodeType() == Node.ELEMENT_NODE) && (childNode.getNodeName().equals("user"))){
          UserName = getAttribute(childNode,"user_name");
          if(UserName.equals(SetUserName)) {
            existUserCheck = true;
            userMap = getNodeItem(childNode.getFirstChild());
            UserNo = userMap.get("user_no");
            AccessToken = userMap.get("access_token");
            AccessTokenSecret = userMap.get("access_token_secret");
            if((UserNo == null) || (AccessToken == null) || (AccessTokenSecret == null)) APIKeyError("Error:APIKey.xml の構文に誤りがあります.");
            if(errorCheck(1)) APIKeyError(ErrorCheckMsg);
          }
        }
        childNode = childNode.getNextSibling();
      }
    } catch (Exception e) {
      e.printStackTrace(pw);
      pw.flush();
      APIKeyError(StackTrace.toString());
    }
    return existUserCheck;
  }
  
  public void addAPIKeyInfo(String AddUserName, String AddAccessToken, String AddAccessTokenSecret) {
    Node apiKeyRoot = document.getDocumentElement();
    Node childNode = apiKeyRoot.getFirstChild();
    Set<String> userNameSet = new HashSet<String>();
    int userCount = 0;
    while(childNode != null) {
      if(childNode.getNodeType() == Node.ELEMENT_NODE) {
        if(childNode.getNodeName().equals("user")) {
          userNameSet.add(getAttribute(childNode, "user_name"));
          userCount++;
        }
        
      }
      childNode = childNode.getNextSibling();
    }
    Element addUserElement = document.createElement("user");
    addUserElement.setAttribute("user_name", AddUserName);
    Element addUserNo = document.createElement("user_no");
    addUserNo.setTextContent(String.valueOf(userCount + 1));
    Element addAccessToken = document.createElement("access_token");
    addAccessToken.setTextContent(AddAccessToken);
    Element addAccessTokenSecret = document.createElement("access_token_secret");
    addAccessTokenSecret.setTextContent(AddAccessTokenSecret);
    
    addUserElement.appendChild(addUserNo);
    addUserElement.appendChild(addAccessToken);
    addUserElement.appendChild(addAccessTokenSecret);
    
    apiKeyRoot.appendChild(addUserElement);
    
    try{
      writeXML();
    }catch (Exception e){
      e.printStackTrace(pw);
      pw.flush();
      APIKeyError(StackTrace.toString());
    }
    
  }
  
  private void writeXML() throws Exception {
    TransformerFactory transFormerFactory = TransformerFactory.newInstance();
    Transformer transformer = transFormerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.METHOD,"xml");
    transformer.setOutputProperty(OutputKeys.INDENT,"yes");
    transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT,"2");
    transformer.transform(new DOMSource(document), new StreamResult(new File(XMLPath)));
  }

  private String deleteIndentXML() {
    String tempFilePath="";
    try {
      String line;
      File path = new File(XMLPath);
      tempFilePath = path.getParent() + "/temp";
      if(path.exists() == false) APIKeyError("Error:APIKey.xml の読み込みに失敗しました.");
      BufferedReader br = new BufferedReader(new FileReader(XMLPath));
      BufferedWriter bw = new BufferedWriter(new FileWriter(tempFilePath));
      while ((line = br.readLine()) != null) {
        bw.write(replaceStr(line, "  ", ""));
      }
      bw.close();
      br.close();
    }catch(Exception e){
      e.printStackTrace(pw);
      pw.flush();
      APIKeyError(pw.toString());
    }
    return tempFilePath;
  }
  
  private boolean errorCheck(int CheckPattern) {
    ErrorCheckMsg = "Error:入力データの参照に失敗しました.";
    int errorCount=0;
    if(CheckPattern == 0) {
      if(!ConsumerKey.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  ConsumerKey を入力してください.";
      }
      if(!ConsumerSecret.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  ConsumerSecret を入力してください.";
      }
    }else if(CheckPattern == 1){
      if(!UserName.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  UserName を入力してください.";
      }
      if(!AccessToken.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  AccessToken を入力してください.";
      }
      if(!AccessTokenSecret.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  AccessTokenSecret を入力してください.";
      }
    }
    
    if(errorCount == 0) {
      ErrorCheckMsg = "";
      return true;
    }else{
      return false;
    }
  }
  
  private void APIKeyError(String msg) {
    System.out.println(msg);
    System.exit(1);
  }
  
  private Map<String, String> getNodeItem(Node node) {
    Map<String,String> nodeItem = new HashMap<String,String>();
    while(node != null) {
      if(node.getNodeType() == Node.ELEMENT_NODE) nodeItem.put(node.getNodeName(), getTextNode(node.getFirstChild()));
      node = node.getNextSibling();
    }
    return nodeItem;
  }
  
  private String getTextNode(Node node) {
    if(node != null) {
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

  private static String replaceStr(String str, String replace, String replacement) {
    String reg = replace;
    Pattern pat = Pattern.compile(reg);
    Matcher mat = pat.matcher(str);
    return mat.replaceAll(replacement);
  }
  
  public boolean existUser(String userName) {
    return readAccessToken(userName);
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















