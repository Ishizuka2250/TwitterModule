package com.TwitterModule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.IDs;
import twitter4j.auth.RequestToken;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.TwitterException;

public class TwitterModule {
  private String ConsumerKey;
  private String ConsumerSecret;
  private String AccessToken;
  private String AccessTokenSecret;
  private SqliteModule sqlite;
  private APIKey key;
  private String APIKeyXMLPath = "D:/twitterApp/APIKey.xml";
  private long TwitterApiStopTimes = (15 * 60 * 1000) + (30 * 1000);// TwitterAPI制限回避→15分
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);

  public TwitterModule(String UserName) throws ClassNotFoundException {
    key = new APIKey(APIKeyXMLPath);
    ConsumerKey = key.getConsumerKey();
    ConsumerSecret = key.getConsumerSecret();
    if(key.existUser(UserName)) {
      AccessToken = key.getAccessToken();
      AccessTokenSecret = key.getAccessTokenSecret();
      sqlite = new SqliteModule(UserName);
    }else{
      twitterOAuthWizard(UserName);
      System.exit(0);
    }
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
      key.addAPIKeyInfo(UserName, AccessToken, AccessTokenSecret);
      System.out.println("Info:[" + UserName + "]を登録しました.");
    }catch(Exception e){
      e.printStackTrace();
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
  
  /* 
   * TwitterIDsに保存されているユーザー情報を更新
   * FilterPattern = 0:TwitterIDsに登録されている全ユーザーを更新
   * FilterPattern = 1:TwitterIDsに登録されているユーザーを更新 (フォロー・フォロワーでもない他人ユーザー かつ アカウントが凍結されているユーザーを除く)
   * FilterPattern = 2:TwitterIDsに登録されているアカウントが凍結されているユーザーを更新
   * */
  public void selectUserInfoUpdate(int FilterPattern) throws TwitterException {
    List<String> IDList = new ArrayList<String>();
    if(FilterPattern == 0) IDList = sqlite.getTwitterIDList(0,0);
    else if(FilterPattern == 1) IDList = sqlite.getTwitterIDList(0,1);
    else if(FilterPattern == 2) IDList = sqlite.getTwitterIDBan();
    else {
      System.out.println("Error:無効なフィルターパターンが検知されました.");
      System.exit(1);
    }
    sqlite.updateFlgsOn(IDList);
    twitterUserInfoUpdate();
  }
  
  /*
   * TwitterIDs.UpdateFlg = 1 のユーザー情報を更新
   */
  public void twitterUserInfoUpdate() throws TwitterException {
    List<String> updateIDList = sqlite.getUpdateUserIDList(1);

    // uploadFlg=1のものを処理
    while (updateIDList.size() != 0) {
      if (getTwitterUserInfo(updateIDList) == true) {
        break;
      } else {
        System.out.println("Info:API制限により処理を中断しました(" + TwitterApiStopTimes + "ms停止)");
        try {
          Thread.sleep(TwitterApiStopTimes);
        } catch (InterruptedException ie) {
          ie.printStackTrace(pw);
          pw.flush();
          System.out.println("InterruptedException:\n" + StackTrace.toString());
        }
        updateIDList = sqlite.getUpdateUserIDList(1);
      }
    }
  }

  // API制限までIDListのユーザー情報を取得する
  public boolean getTwitterUserInfo(List<String> IDList) throws TwitterException {
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    Map<String, String> UserInfoMap = new HashMap<String, String>();
    Boolean APILimit = false;
    String buffer = "";
    long getUserInfoOK = 0L;
    long onHold = 0L;
    for (String id : IDList) {
      if (!APILimit) {
        buffer = getUserInfo(twitter, id);
        //getUserInfoから-1が返された場合API制限により一時的にUser情報取得不可
        //-1 以外の場合にマップへユーザー情報を格納
        if (!buffer.equals("-1")) {
          UserInfoMap.put(id, buffer);
          getUserInfoOK++;
        //-1 の場合, ユーザ情報未取得のユーザ数をカウントする
        } else {
          onHold++;
          APILimit = true;
        }
      } else {
        onHold++;
      }
    }
    //TwitterAPI制限による中断・全ユーザー情報取得完了時にSqliteへユーザ情報を格納する
    sqlite.updateUserInfo(UserInfoMap);
    System.out.println("Info:ユーザー取得完了 -- " + getUserInfoOK + " 保留 -- " + onHold);
    
    //getUserInfoから-1を受け取った場合ユーザー取得処理を中断する
    if (buffer.equals("-1")) {
      return false;
    }
    System.out.println("Info:フォロワーIDの取得が完了しました.");
    return true;
  }

  // TwitterIDに該当するユーザー情報を取得し、Sqlite挿入用のCSVデータを作成する
  public String getUserInfo(Twitter twitter, String idStr) {
    long id = Long.parseLong(idStr);
    try {
      User user = twitter.showUser(id);
      String userName = replaceStr(user.getName(), "'", "''");
      String userScreenName = replaceStr(user.getScreenName(), "'", "''");
      String userDescription = replaceStr(user.getDescription(), "'", "''");
      String userTweetCount = String.valueOf(user.getStatusesCount());
      String userFollowCount = String.valueOf(user.getFriendsCount());
      String userFollowerCount = String.valueOf(user.getFollowersCount());
      String userFavouritesCount = String.valueOf(user.getFavouritesCount());
      String userLocation = replaceStr(user.getLocation(), "'", "''");
      String userCreatedAt = String.valueOf(user.getCreatedAt());
      String userBackGroundColor = String.valueOf(user.getProfileBackgroundColor());
      System.out.println("Info:[" + userName + "] の情報を取得しました. ");
      
      return "'" + id + "','" + userName + "','" + userScreenName + "','"
          + userDescription + "','" + userTweetCount + "','" + userFollowCount
          + "','" + userFollowerCount + "','" + userFavouritesCount + "','"
          + userLocation + "','" + userCreatedAt + "','" + userBackGroundColor + "'";
    } catch (TwitterException te) {
      if ((te.getErrorCode() == 63) || (te.getErrorCode() == 50)) {
        System.out.println("Info:[" + id + "] 凍結中のユーザーアカウントです. ");
        return "1";
      } else if (te.getErrorCode() == 88) {
        System.out.println("Info:API制限に到達しました。5分間停止します。");
        return "-1";
      } else {
        te.printStackTrace(pw);
        pw.flush();
        System.out.println("Error:アカウント情報取得中に問題が発生しました. \n" + StackTrace.toString());
        System.exit(1);
      }
      return "";
    }
  }

  /*public static void twitterRemoveUser(List<String> TwitterUserSet) {
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    try{
      for(String id : TwitterUserSet) {
        twitter.destroyFriendship();
      }
    }catch(TwitterException e) {
      
    }
    
  }*/
  
  public String inputString() {
    String input;
    BufferedReader buf = new BufferedReader(new InputStreamReader(System.in), 1);
    try {
      System.out.print("tweet:");
      input = buf.readLine();
    } catch (IOException e) {
      System.out.println("Error:IOException");
      return "";
    }
    return input;
  }

  public void twitterAddUserIDCheck() throws TwitterException {
    Boolean follow,follower;
    Set<String> nowFollowIDSet = new HashSet<String>();
    Set<String> nowFollowerIDSet = new HashSet<String>();
    Set<String> nowTwitterIDSet = new HashSet<String>();
    Set<String> oldTwitterIDSet = new HashSet<String>();
    
    Map<String,String> addFollowIDMap = new HashMap<String,String>();
    Map<String,String> addFollowerIDMap = new HashMap<String,String>();
    Map<String, String> addTwitterIDMap = new HashMap<String, String>();
    List<String> addTwitterIDList = new ArrayList<String>();

    //TwitterAPIより現在フォローしているユーザーを取得
    getTwitterFollowIDList().forEach((v) -> {
        nowFollowIDSet.add(v);
        nowTwitterIDSet.add(v);
    });
    //TwitterAPIより現在フォローされているユーザーを取得
    getTwitterFollowerIDList().forEach((v) -> {
        nowFollowerIDSet.add(v);
        nowTwitterIDSet.add(v);
    });
    
    //TwitterIDsに登録されている全TwitterIDを取得する
    sqlite.getTwitterIDList(0,0).forEach((v) -> {oldTwitterIDSet.add(v);});
    
    for (String id : nowTwitterIDSet) {
      //Sqliteに保存されていない新規IDのチェック
      if(!oldTwitterIDSet.contains(id)) {
        //0:フォロー, 1:フォローしていない
        if(nowFollowIDSet.contains(id)) {
          addFollowIDMap.put(id, "0");
          follow = true;
        }else{
          addFollowIDMap.put(id, "1");
          follow = false;
        }
        //0:フォロワー, 1:フォロワーではない 
        if(nowFollowerIDSet.contains(id)){
          addFollowerIDMap.put(id, "0");
          follower = true;
        }else{
          addFollowerIDMap.put(id, "1");
          follower = false;
        }
        //フォローしていない かつ フォロワーでもない場合
        if((!follow) && (!follower)) {
          addTwitterIDMap.put(id, "1");
        }else{
          addTwitterIDMap.put(id, "0");
        }
      }
    }
    addTwitterIDMap.forEach((k,v) -> {addTwitterIDList.add(k);});

    //Twitterで取得した各ID情報をDB(Sqlite)へ保存
    if (addFollowIDMap.size() != 0) {
      sqlite.updateTwitterID(addFollowIDMap, 1);
      System.out.println("Info:TwitterFollowIDs -- " + addFollowIDMap.size() + "件 追加");
      //addFollowIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
    }
    if (addFollowerIDMap.size() != 0) {
      sqlite.updateTwitterID(addFollowerIDMap, 2);
      System.out.println("Info:TwitterFollowerIDs -- " + addFollowerIDMap.size() + "件 追加");
      //addFollowerIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
    }
      
    if (addTwitterIDMap.size() != 0) {
      sqlite.updateTwitterID(addTwitterIDMap, 0);
      System.out.println("Info:TwitterIDs update -- " + addTwitterIDMap.size() + "件　追加");
      sqlite.updateFlgsOn(addTwitterIDList);
      twitterUserInfoUpdate();
    }
    
    if ((addFollowIDMap.size() == 0) && (addFollowerIDMap.size() == 0)
        && (addTwitterIDMap.size() == 0)) System.out.println("Info:新規追加IDはありません.");
  }

  public void twitterUpdateUserIDCheck() throws TwitterException {
    Set<String> nowFollowIDSet = new HashSet<String>();         //TwitterAPIから取得したフォローしているユーザID
    Set<String> nowFollowerIDSet = new HashSet<String>();       //TwitterAPIから取得したフォローされているユーザID
    Set<String> oldFollowIDSet = new HashSet<String>();         //DBから取得したフォローしているユーザID
    Set<String> oldFollowerIDSet = new HashSet<String>();       //DBから取得したフォローされているユーザID
    Set<String> oldRemoveFollowIDSet = new HashSet<String>();   //DBから取得したフォローしていないユーザID
    Set<String> oldRemoveFollowerIDSet = new HashSet<String>(); //DBから取得したフォローから外された(リムーブされた)ユーザID
    
    Map<String,String> followIDMap = new HashMap<String,String>();
    Map<String,String> followerIDMap = new HashMap<String,String>();
    Set<String> notRemoveTwitterIDSet = new HashSet<String>();
    Map<String,String> notRemoveTwitterIDMap = new HashMap<String,String>();
    Map<String,String> removeFollowIDMap = new HashMap<String,String>();
    Map<String,String> removeFollowerIDMap = new HashMap<String,String>();
    Set<String> removeTwitterIDSet = new HashSet<String>();
    Map<String,String> removeTwitterIDMap = new HashMap<String,String>();
    
    getTwitterFollowIDList().forEach((v) -> {nowFollowIDSet.add(v);});
    getTwitterFollowerIDList().forEach((v) -> {nowFollowerIDSet.add(v);});
    
    sqlite.getTwitterIDList(1,1).forEach((v) -> {oldFollowIDSet.add(v);});
    sqlite.getTwitterIDList(2,1).forEach((v) -> {oldFollowerIDSet.add(v);});
    sqlite.getTwitterIDList(1,2).forEach((v) -> {oldRemoveFollowIDSet.add(v);});
    sqlite.getTwitterIDList(2,2).forEach((v) -> {oldRemoveFollowerIDSet.add(v);});
    
    for(String id : oldFollowIDSet) {
      if(!nowFollowIDSet.contains(id)) removeFollowIDMap.put(id,"1");
    }
    for(String id : nowFollowIDSet) {
      if(!oldFollowIDSet.contains(id)) followIDMap.put(id,"0");
    }
    
    for(String id : oldFollowerIDSet) {
      if(!nowFollowerIDSet.contains(id)) removeFollowerIDMap.put(id,"1");
    }
    for(String id : nowFollowerIDSet) {
      if(!oldFollowerIDSet.contains(id)) followerIDMap.put(id,"0");
    }
    
    //リムーブ・リフォローされたユーザーに対しsqlite内のNotFollow/RemoveFollowerをチェックしてどちらも1であればTwitterIDsのRemoveFlg=1にセットする
    for(String id : removeFollowIDMap.keySet()) {
      if((removeFollowerIDMap.get(id) != null) || (oldRemoveFollowerIDSet.contains(id))) {
        removeTwitterIDSet.add(id);
      }
    }
    for(String id : removeFollowerIDMap.keySet()) {
      if((removeFollowIDMap.get(id) != null) || (oldRemoveFollowIDSet.contains(id))) {
        removeTwitterIDSet.add(id);
      }
    }
    removeTwitterIDSet.forEach((v) -> {removeTwitterIDMap.put(v, "1");});
    
    
    for(String id : followIDMap.keySet()) {
      notRemoveTwitterIDSet.add(id);
    }
    for(String id : followerIDMap.keySet()) {
      notRemoveTwitterIDSet.add(id);
    }
    notRemoveTwitterIDSet.forEach((v) -> {notRemoveTwitterIDMap.put(v, "0");});
    
    //Sqliteを更新
    if(removeFollowIDMap.size() != 0) {
      sqlite.updateTwitterID(removeFollowIDMap, 1);
      System.out.println("Info:フォローを外したユーザ");
      removeFollowIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(removeFollowerIDMap.size() != 0) {
      sqlite.updateTwitterID(removeFollowerIDMap, 2);
      System.out.println("Info:フォローを外されたユーザ");
      removeFollowerIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(removeTwitterIDMap.size() != 0) {
      sqlite.updateTwitterID(removeTwitterIDMap, 0);
      System.out.println("Info:フォローを外した & フォローを外されたユーザ");
      removeTwitterIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    
    if(followIDMap.size() != 0) {
      sqlite.updateTwitterID(followIDMap, 1);
      System.out.println("Info:フォローしたユーザ");
      followIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(followerIDMap.size() != 0) {
      sqlite.updateTwitterID(followerIDMap, 2);
      System.out.println("Info:フォローされたユーザ");
      followerIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(notRemoveTwitterIDMap.size() != 0) {
      sqlite.updateTwitterID(notRemoveTwitterIDMap, 0);
      //System.out.println("Info:再度フォローした もしくは 再度フォローされたユーザ");
      //notRemoveTwitterIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    
    int updateSize = removeFollowIDMap.size() + removeFollowerIDMap.size() + removeTwitterIDMap.size()
        + followIDMap.size() + followerIDMap.size() + notRemoveTwitterIDMap.size();
    if(updateSize == 0) System.out.println("Info:リムーブ・更新対象のIDはありません.");
  }

  public List<String> getTwitterFollowerIDList() throws TwitterException {
    long cursol = -1L;
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    IDs ids;
    List<String> IDList = new ArrayList<String>();
    do {
      ids = twitter.getFollowersIDs(cursol);
      for (long temp : ids.getIDs()) {
        IDList.add(String.valueOf(temp));
      }
    } while (ids.hasNext());
    return IDList;
  }

  public List<String> getTwitterFollowIDList() throws TwitterException {
    long cursol = -1L;
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    IDs ids;
    List<String> IDList = new ArrayList<String>();
    do {
      ids = twitter.getFriendsIDs(cursol);
      for (long temp : ids.getIDs()) {
        IDList.add(String.valueOf(temp));
      }
    } while (ids.hasNext());
    return IDList;
  }

  /*public void outputText(List<String> list) throws IOException {
    File file = new File("output.txt");
    PrintWriter printWriter = new PrintWriter(new BufferedWriter(
        new FileWriter(file)));
    for (String info : list) {
      printWriter.println(info);
    }
    printWriter.close();
  }*/

  private static String replaceStr(String str, String replace, String replacement) {
    String reg = replace;
    Pattern pat = Pattern.compile(reg);
    Matcher mat = pat.matcher(str);
    return mat.replaceAll(replacement);
  }

  /*private String cutString(String str, String delimiter, int field) {
    int n = 0;
    for (String temp : str.split(delimiter)) {
      if (field == n)
        return temp;
      else
        n++;
    }
    return "";
  }

  private String StringToUnicode(String str) {
    String unicodeStr = "";
    for (char temp : str.toCharArray()) {
      unicodeStr += "\\u" + Integer.toHexString((int) temp);
    }
    return unicodeStr;
  }*/

}
