package com.TwitterModule.Twitter;

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

/**
 * APIキーファイル操作クラス<br>
 * TwitterのAPIキーを格納したファイル(APIKey.xml)から以下のKey情報を読み込む<br>
 * - ConsumerKey<br>
 * - ConsumerSecret<br>
 * - AccessToken<br>
 * - AccessTokenSecret<br>
 * @author ishizuka
 *
 */
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
  
  /**
   * APIKeyファイル操作クラスのコンストラクタ<br>
   * APIKeyファイルの読み込み後, ConsumerKey ConsumerSecret を取り込む.<br>
   * @param APIKeyXmlPath APIKeyファイルの格納先パス
   */
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
  
  /**
   * ConsumerKey ConsumerSecret を読み込む.
   */
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
          if((ConsumerKey == null) || (ConsumerSecret == null)) APIKeyError("Error:ConsumerKey または ConsumerSecret の読み取りに失敗しました."); 
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
  
  /**
   * ユーザ名を指定し, AccessToken AccessTokenSecret の読み込みを行う.<br>
   * 指定したユーザが存在する場合 True を返し, 存在しない場合 False を返す. <br>
   * @param SetUserName Twitterユーザ名
   * @return ユーザの存在有無
   */
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
            if((UserNo == null) || (AccessToken == null) || (AccessTokenSecret == null))
              APIKeyError("Error:UserName, AccessToken, AccessTokenSecret のいずれかの読み取りに失敗しました.");
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
  
  /**
   * 指定されたユーザの AccessToken AccessTokenSecret をAPIKeyファイルに追加する.<br>
   * 同名のユーザが存在しない場合, 追加処理を実行し True を返す.<br>
   * 同名のユーザが存在する場合, 追加処理を中止し  false を返す.<br>
   * @param AddUserName ユーザ名
   * @param AddAccessToken AccessToken
   * @param AddAccessTokenSecret AccessTokenSecret
   * @return APIKey情報の追加結果
   */
  public boolean addAPIKeyInfo(String AddUserName, String AddAccessToken, String AddAccessTokenSecret) {
    Node apiKeyRoot = document.getDocumentElement();
    Node childNode = apiKeyRoot.getFirstChild();
    Set<String> userNameSet = new HashSet<String>();
    boolean addAPIKeyResult = true;
    int userCount = 0;
    
    if(existUser(AddUserName) == false) { 
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
    }else{
      addAPIKeyResult = false;
    }
    return addAPIKeyResult;
  }
  
  /**
   * APIKeyファイルの書き込みを行う. 
   * @throws Exception
   */
  private void writeXML() throws Exception {
    TransformerFactory transFormerFactory = TransformerFactory.newInstance();
    Transformer transformer = transFormerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.METHOD,"xml");
    transformer.setOutputProperty(OutputKeys.INDENT,"yes");
    transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT,"2");
    transformer.transform(new DOMSource(document), new StreamResult(new File(XMLPath)));
  }
  
  /**
   * インデントを削除したAPIKeyファイルの内容をファイル出力する.<br>
   * 出力先:APIKeyファイルと同じディレクトリ<br>
   * ファイル名:temp<br>
   * ※Transform後のインデント崩れ対策に使用<br>
   * @return 出力したファイルのパス
   */
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
  
  /**
   * 指定されたパターン(CheckPattern)に従い.<br>
   * ConsumerKey, ConsumerSecret, UserName, AccessToken, AccessTokenSecret <br>
   * のいずれかに値が入っているかチェックし, なければ false を返しエラーメッセージを構築する.<br>
   * 指定された値が存在する場合は true を返す.<br>
   * CheckPattern:<br>
   * 0 -- ConsumerKey, ConsumerSecret を対象としたチェック<br>
   * 1 -- UserName, AccessToken, AccessTokenSecret を対象としたチェック<br>
   * @param CheckPattern データチェックパターン
   * @return エラーチェック結果
   */
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
    }else{
      APIKeyError("Error:未定義のCheckPatternが指定されました.");
    }
    
    if(errorCount == 0) {
      ErrorCheckMsg = "";
      return true;
    }else{
      return false;
    }
  }
  
  /**
   * エラーメッセージを出力して処理を強制終了する.
   * @param msg エラーメッセージ
   */
  private void APIKeyError(String msg) {
    System.out.println(msg);
    System.exit(1);
  }
  
  /**
   * 親ノード配下の子ノードを  Map(ノード名, テキストノードの文字列データ) で返却する.
   * @param ParentNode 親ノード
   * @return
   */
  private Map<String, String> getNodeItem(Node ParentNode) {
    Map<String,String> nodeItem = new HashMap<String,String>();
    while(ParentNode != null) {
      if(ParentNode.getNodeType() == Node.ELEMENT_NODE) nodeItem.put(ParentNode.getNodeName(), getTextNode(ParentNode));
      ParentNode = ParentNode.getNextSibling();
    }
    return nodeItem;
  }
  
  /**
   * 要素ノードのテキストノードを文字列として返却する.<br>
   * 要素ノード・テキストノード意外が与えられた場合は空文字を返す.
   * @param node 要素ノード
   * @return
   */
  private String getTextNode(Node node) {
    if(node.getNodeType() == Node.ELEMENT_NODE) return node.getFirstChild().getNodeValue();
    else if(node.getNodeType() == Node.TEXT_NODE) return node.getNodeValue();
    else return "";
  }
  
  /**
   * 要素ノードが持つ属性を文字列として返却する.<br>
   * 属性を持たない・要素ノードではない場合, 空文字を返却する.
   * @param node 要素ノード
   * @param id 属性名
   * @return
   */
  private String getAttribute(Node node, String id) {
    NamedNodeMap attributes = node.getAttributes();
    Node attribute;
    if((node.getNodeType() == Node.ELEMENT_NODE) && (attributes != null)) {
      attribute = attributes.getNamedItem(id);
      if(attribute != null) return attribute.getNodeValue();
      else return "";
    }else{
      return "";
    }
  }

  /**
   * 一致する文字を全て置換し, その結果を返却する.
   * @param str 置換対象の文字列
   * @param replace 置換する文字
   * @param replacement 置換後の文字
   * @return 置換結果文字列
   */
  private static String replaceStr(String str, String replace, String replacement) {
    String reg = replace;
    Pattern pat = Pattern.compile(reg);
    Matcher mat = pat.matcher(str);
    return mat.replaceAll(replacement);
  }
  
  /**
   * APIKeyファイル内に指定されたユーザが存在すれば True, 存在しなければ False を返却する. 
   * @param userName ユーザ名
   * @return ユーザ名のチェック結果
   */
  public boolean existUser(String userName) {
    return readAccessToken(userName);
  }
  
  /**
   * APIKeyファイルから読み込んだ ConsumerKey を返却する.
   * @return ConsumerKey
   */
  public String getConsumerKey() {
    return ConsumerKey;
  }
  
  /**
   * APIKeyファイルから読み込んだ ConsumerKeySecret を返却する.
   * @return ConsumerKeySecret
   */
  public String getConsumerSecret() {
    return ConsumerSecret;
  }
  
  /**
   * APIKeyファイルから読み込んだ AccessToken を返却する.
   * @return AccessToken
   */
  public String getAccessToken() {
    return AccessToken;
  }
  
  /**
   * APIKeyファイルから読み込んだ AccessTokenSecret を返却する.
   * @return AccessTokenSecret
   */
  public String getAccessTokenSecret() {
    return AccessTokenSecret;
  }
  
}















