package com.TwitterModule.Twitter;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.TwitterException;

public class TwitterCore {
  private String APIKeyXMLPath;
  private APIKey Key;
  private String ConsumerKey;
  private String ConsumerSecret;
  private String AccessToken;
  private String AccessTokenSecret;
  
  public TwitterCore(String UserName) {
    String cd = new File(".").getAbsoluteFile().getParent();
    APIKeyXMLPath = cd + "\\APIKey.xml";
    Key = new APIKey(APIKeyXMLPath);
    ConsumerKey = Key.getConsumerKey();
    ConsumerSecret = Key.getConsumerSecret();
    if(Key.existUser(UserName)) {
      AccessToken = Key.getAccessToken();
      AccessTokenSecret = Key.getAccessTokenSecret();
    }else{
      twitterOAuthWizard(UserName);
      System.exit(0);
    }
  }
 
  private ConfigurationBuilder twitterConfigure() {
    ConfigurationBuilder confbuilder = new ConfigurationBuilder();
    confbuilder.setDebugEnabled(true);
    confbuilder.setOAuthConsumerKey(ConsumerKey);
    confbuilder.setOAuthConsumerSecret(ConsumerSecret);
    confbuilder.setOAuthAccessToken(AccessToken);
    confbuilder.setOAuthAccessTokenSecret(AccessTokenSecret);
    return confbuilder;
  }
  
  public Twitter getTwitterInstance() {
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    return twitterFactory.getInstance();
  }

  private void twitterOAuthWizard(String UserName) {
    try {
      Twitter twitter = new TwitterFactory().getInstance();
      twitter.setOAuthConsumer(ConsumerKey, ConsumerSecret);
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      
      System.out.print("Info:未登録のユーザーです. [" + UserName + "]を新規登録しますか(y/n)？ -- ");
      String ans = br.readLine();
      if(!ans.equals("y")) System.exit(0);
      
      RequestToken requestToken= twitter.getOAuthRequestToken();
      System.out.println("Info:RequestToken, RequestTokenSecret を取得しました. \n" +
        "-> [RequestToken]" + requestToken.getToken() + "\n" +
        "-> [RequestTokenSecret]" + requestToken.getTokenSecret());
      AccessToken accessToken = null;
      while (accessToken == null) {
        System.out.println("Info:下記のURLにアクセスし, 任意のTwitterアカウントで認証してください.");
        System.out.println(requestToken.getAuthenticationURL());
        System.out.print("Info:表示されたPINコードを入力してください. -- ");
        String PINCode = br.readLine();
        try {
          if(PINCode.length() > 0) {
            accessToken = twitter.getOAuthAccessToken(requestToken,PINCode);
          }else{
            accessToken = twitter.getOAuthAccessToken(requestToken);
          }
        }catch(TwitterException te) {
          te.printStackTrace();
        }
      }
      AccessToken = accessToken.getToken();
      AccessTokenSecret = accessToken.getTokenSecret();
      System.out.println("Info:AccessToken, AccessTokenSecret を取得しました. \n" +
        "-> [AccessToken]" + AccessToken + "\n" +
        "-> [AccessTokenSecret]" + AccessTokenSecret);
      Key.addAPIKeyInfo(UserName, AccessToken, AccessTokenSecret);
      System.out.println("Info:[" + UserName + "]を登録しました.");
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  

}
