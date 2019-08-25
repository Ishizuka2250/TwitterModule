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
 * API�L�[�t�@�C������N���X<br>
 * Twitter��API�L�[���i�[�����t�@�C��(APIKey.xml)����ȉ���Key����ǂݍ���<br>
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
   * APIKey�t�@�C������N���X�̃R���X�g���N�^<br>
   * APIKey�t�@�C���̓ǂݍ��݌�, ConsumerKey ConsumerSecret ����荞��.<br>
   * @param APIKeyXmlPath APIKey�t�@�C���̊i�[��p�X
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
      System.out.println("Info:APIKey.xml�̓ǂݍ��� -- OK");
      
    }catch (Exception e){
      e.printStackTrace(pw);
      pw.flush();
      APIKeyError(StackTrace.toString());
    }
  }
  
  /**
   * ConsumerKey ConsumerSecret ��ǂݍ���.
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
          if((ConsumerKey == null) || (ConsumerSecret == null)) APIKeyError("Error:ConsumerKey �܂��� ConsumerSecret �̓ǂݎ��Ɏ��s���܂���."); 
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
   * ���[�U�����w�肵, AccessToken AccessTokenSecret �̓ǂݍ��݂��s��.<br>
   * �w�肵�����[�U�����݂���ꍇ True ��Ԃ�, ���݂��Ȃ��ꍇ False ��Ԃ�. <br>
   * @param SetUserName Twitter���[�U��
   * @return ���[�U�̑��ݗL��
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
              APIKeyError("Error:UserName, AccessToken, AccessTokenSecret �̂����ꂩ�̓ǂݎ��Ɏ��s���܂���.");
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
   * �w�肳�ꂽ���[�U�� AccessToken AccessTokenSecret ��APIKey�t�@�C���ɒǉ�����.<br>
   * �����̃��[�U�����݂��Ȃ��ꍇ, �ǉ����������s�� True ��Ԃ�.<br>
   * �����̃��[�U�����݂���ꍇ, �ǉ������𒆎~��  false ��Ԃ�.<br>
   * @param AddUserName ���[�U��
   * @param AddAccessToken AccessToken
   * @param AddAccessTokenSecret AccessTokenSecret
   * @return APIKey���̒ǉ�����
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
   * APIKey�t�@�C���̏������݂��s��. 
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
   * �C���f���g���폜����APIKey�t�@�C���̓��e���t�@�C���o�͂���.<br>
   * �o�͐�:APIKey�t�@�C���Ɠ����f�B���N�g��<br>
   * �t�@�C����:temp<br>
   * ��Transform��̃C���f���g����΍�Ɏg�p<br>
   * @return �o�͂����t�@�C���̃p�X
   */
  private String deleteIndentXML() {
    String tempFilePath="";
    try {
      String line;
      File path = new File(XMLPath);
      tempFilePath = path.getParent() + "/temp";
      if(path.exists() == false) APIKeyError("Error:APIKey.xml �̓ǂݍ��݂Ɏ��s���܂���.");
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
   * �w�肳�ꂽ�p�^�[��(CheckPattern)�ɏ]��.<br>
   * ConsumerKey, ConsumerSecret, UserName, AccessToken, AccessTokenSecret <br>
   * �̂����ꂩ�ɒl�������Ă��邩�`�F�b�N��, �Ȃ���� false ��Ԃ��G���[���b�Z�[�W���\�z����.<br>
   * �w�肳�ꂽ�l�����݂���ꍇ�� true ��Ԃ�.<br>
   * CheckPattern:<br>
   * 0 -- ConsumerKey, ConsumerSecret ��ΏۂƂ����`�F�b�N<br>
   * 1 -- UserName, AccessToken, AccessTokenSecret ��ΏۂƂ����`�F�b�N<br>
   * @param CheckPattern �f�[�^�`�F�b�N�p�^�[��
   * @return �G���[�`�F�b�N����
   */
  private boolean errorCheck(int CheckPattern) {
    ErrorCheckMsg = "Error:���̓f�[�^�̎Q�ƂɎ��s���܂���.";
    int errorCount=0;
    if(CheckPattern == 0) {
      if(!ConsumerKey.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  ConsumerKey ����͂��Ă�������.";
      }
      if(!ConsumerSecret.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  ConsumerSecret ����͂��Ă�������.";
      }
    }else if(CheckPattern == 1){
      if(!UserName.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  UserName ����͂��Ă�������.";
      }
      if(!AccessToken.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  AccessToken ����͂��Ă�������.";
      }
      if(!AccessTokenSecret.equals("")) {
        errorCount++;
        ErrorCheckMsg = ErrorCheckMsg + "\n" + "  AccessTokenSecret ����͂��Ă�������.";
      }
    }else{
      APIKeyError("Error:����`��CheckPattern���w�肳��܂���.");
    }
    
    if(errorCount == 0) {
      ErrorCheckMsg = "";
      return true;
    }else{
      return false;
    }
  }
  
  /**
   * �G���[���b�Z�[�W���o�͂��ď����������I������.
   * @param msg �G���[���b�Z�[�W
   */
  private void APIKeyError(String msg) {
    System.out.println(msg);
    System.exit(1);
  }
  
  /**
   * �e�m�[�h�z���̎q�m�[�h��  Map(�m�[�h��, �e�L�X�g�m�[�h�̕�����f�[�^) �ŕԋp����.
   * @param ParentNode �e�m�[�h
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
   * �v�f�m�[�h�̃e�L�X�g�m�[�h�𕶎���Ƃ��ĕԋp����.<br>
   * �v�f�m�[�h�E�e�L�X�g�m�[�h�ӊO���^����ꂽ�ꍇ�͋󕶎���Ԃ�.
   * @param node �v�f�m�[�h
   * @return
   */
  private String getTextNode(Node node) {
    if(node.getNodeType() == Node.ELEMENT_NODE) return node.getFirstChild().getNodeValue();
    else if(node.getNodeType() == Node.TEXT_NODE) return node.getNodeValue();
    else return "";
  }
  
  /**
   * �v�f�m�[�h���������𕶎���Ƃ��ĕԋp����.<br>
   * �����������Ȃ��E�v�f�m�[�h�ł͂Ȃ��ꍇ, �󕶎���ԋp����.
   * @param node �v�f�m�[�h
   * @param id ������
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
   * ��v���镶����S�Ēu����, ���̌��ʂ�ԋp����.
   * @param str �u���Ώۂ̕�����
   * @param replace �u�����镶��
   * @param replacement �u����̕���
   * @return �u�����ʕ�����
   */
  private static String replaceStr(String str, String replace, String replacement) {
    String reg = replace;
    Pattern pat = Pattern.compile(reg);
    Matcher mat = pat.matcher(str);
    return mat.replaceAll(replacement);
  }
  
  /**
   * APIKey�t�@�C�����Ɏw�肳�ꂽ���[�U�����݂���� True, ���݂��Ȃ���� False ��ԋp����. 
   * @param userName ���[�U��
   * @return ���[�U���̃`�F�b�N����
   */
  public boolean existUser(String userName) {
    return readAccessToken(userName);
  }
  
  /**
   * APIKey�t�@�C������ǂݍ��� ConsumerKey ��ԋp����.
   * @return ConsumerKey
   */
  public String getConsumerKey() {
    return ConsumerKey;
  }
  
  /**
   * APIKey�t�@�C������ǂݍ��� ConsumerKeySecret ��ԋp����.
   * @return ConsumerKeySecret
   */
  public String getConsumerSecret() {
    return ConsumerSecret;
  }
  
  /**
   * APIKey�t�@�C������ǂݍ��� AccessToken ��ԋp����.
   * @return AccessToken
   */
  public String getAccessToken() {
    return AccessToken;
  }
  
  /**
   * APIKey�t�@�C������ǂݍ��� AccessTokenSecret ��ԋp����.
   * @return AccessTokenSecret
   */
  public String getAccessTokenSecret() {
    return AccessTokenSecret;
  }
  
}















