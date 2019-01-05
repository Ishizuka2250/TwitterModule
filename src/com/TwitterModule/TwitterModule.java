package com.TwitterModule;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;

import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

public class TwitterModule {
  private static String ConsumerKey;
  private static String ConsumerSecret;
  private static String AccessToken;
  private static String AccessTokenSecret;
  private static String UserName;
  private static SqliteResource sqlite;
  private static APIKey key;
  private static String APIKeyPath = "D:/twitterApp/APIKey.xml";
  //private static String APIKeyPath = "/home/ishizuka/Temp/TwitterModule/APIKey.xml";
  private static long TwitterApiStopTimes = (15 * 60 * 1000) + (30 * 1000);// TwitterAPI制限回避→15分

  public static void main(String args[]) throws TwitterException, IOException,ClassNotFoundException {
    long start, end, TwitterIDsTime;
    initTwitterModule();
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    
    start = System.currentTimeMillis();
    //新規追加ユーザーのチェック
    //twitterAddUserIDCheck();
    //フォロー・リフォローのチェック
    twitterRemoveUserIDCheck();
    end = System.currentTimeMillis();
    TwitterIDsTime = end - start;
    System.out.println(TwitterIDsTime + "ms");
    System.out.println("done!");
  }
  
  private static void initTwitterModule() throws ClassNotFoundException {
    key = new APIKey(APIKeyPath);
    ConsumerKey = key.getConsumerKey();
    ConsumerSecret = key.getConsumerSecret();
    AccessToken = key.getAccessToken();
    AccessTokenSecret = key.getAccessTokenSecret();
    UserName = key.getUserName();
    sqlite = new SqliteResource(UserName);
  }

  public static ConfigurationBuilder twitterConfigure() {
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
  public static void selectUserInfoUpdate(int FilterPattern) throws TwitterException {
    List<String> IDList = new ArrayList<String>();
    if(FilterPattern == 0) IDList = sqlite.getTwitterIDList(0,0);
    else if(FilterPattern == 1) IDList = sqlite.getTwitterIDList(0,1);
    else if(FilterPattern == 2) IDList = sqlite.getTwitterIDBan();
    //else if(FilterPattern == 3) IDList = sqlite.getTwitterID
    else {
      System.out.println("error:Unknown FilterPattern.");
      System.exit(1);
    }
    sqlite.updateFlgsOn(IDList);
    twitterUserInfoUpdate();
  }
  
  /*
   * TwitterIDs.UpdateFlg = 1 のユーザー情報を更新
   */
  public static void twitterUserInfoUpdate() throws TwitterException {
    List<String> updateIDList = sqlite.getUpdateUserIDList(1);

    // uploadFlg=1のものを処理
    while (updateIDList.size() != 0) {
      if (getTwitterUserInfo(updateIDList) == true) {
        break;
      } else {
        System.out.println("API制限により処理を中断しました。\n-> " + TwitterApiStopTimes + "ms停止");
        try {
          Thread.sleep(TwitterApiStopTimes);
        } catch (InterruptedException e) {}
        updateIDList = sqlite.getUpdateUserIDList(1);
      }
    }
  }

  // API制限までIDListのユーザー情報を取得する
  public static boolean getTwitterUserInfo(List<String> IDList) throws TwitterException {
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    Map<String, String> UserInfoMap = new HashMap<String, String>();
    List<String> OnHoldUserList = new ArrayList<String>();
    Boolean APILimit = false;
    String buffer = "";
    long getUserInfoOK = 0L;
    long onHold = 0L;
    // hashmapを使うb
    for (String id : IDList) {
      if (!APILimit) {
        buffer = getUserInfo(twitter, id);
        if (!buffer.equals("-1")) {
          UserInfoMap.put(id, buffer);
          getUserInfoOK++;
        } else {
          OnHoldUserList.add(id);
          onHold++;
          APILimit = true;
        }
      } else {
        OnHoldUserList.add(id);
        onHold++;
      }
    }
    // updateのしょり
    System.out.println("ユーザー取得完了：" + getUserInfoOK + "\n保留：" + onHold);
    // sqlite.insertBufferTwitterIDs(OnHoldUserList);
    sqlite.updateUserInfo(UserInfoMap);

    if (buffer.equals("-1")) {
      return false;
    }
    System.out.println("フォロワーIDの取得が完了しました。");
    return true;
  }

  // 与えられたTwitterIDに該当するユーザー情報をカンマ区切りで返す
  public static String getUserInfo(Twitter twitter, String idStr) {
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
      System.out.println(id + ":[" + userName + "]の情報を取得しました。 ");
      return "'" + id + "','" + userName + "','" + userScreenName + "','"
          + userDescription + "','" + userTweetCount + "','" + userFollowCount
          + "','" + userFollowerCount + "','" + userFavouritesCount + "','"
          + userLocation + "','" + userCreatedAt + "','" + userBackGroundColor + "'";
    } catch (TwitterException te) {
      if ((te.getErrorCode() == 63) || (te.getErrorCode() == 50)) {
        System.out.println("[" + id + "] -> 凍結中のユーザーアカウントです。 ");
        return "BAN";
      } else if (te.getErrorCode() == 88) {
        // System.out.println("-> API制限に到達しました。5分間停止します。");
        return "-1";
      } else {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        te.printStackTrace(pw);
        pw.flush();
        System.out.println(sw);
        System.exit(1);
      }
      return "";
    }
  }

  public static String inputString() {
    String input;
    BufferedReader buf = new BufferedReader(new InputStreamReader(System.in), 1);
    try {
      System.out.print("tweet:");
      input = buf.readLine();
    } catch (IOException e) {
      System.out.println("error:IOException");
      return "";
    }
    return input;
  }

  public static void twitterAddUserIDCheck() throws TwitterException {
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
      //既存のID情報に存在しない新規IDのチェック
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
      addFollowIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
      System.out.println("TwitterFollowIDs update -- ok");
    }
    if (addFollowerIDMap.size() != 0) {
      sqlite.updateTwitterID(addFollowerIDMap, 2);
      addFollowerIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
      System.out.println("TwitterFollowerIDs update -- ok");
    }
      
    if (addTwitterIDMap.size() != 0) {
      sqlite.updateTwitterID(addTwitterIDMap, 0);
      System.out.println("TwitterIDs update -- ok");
      sqlite.updateFlgsOn(addTwitterIDList);
      twitterUserInfoUpdate();
    }
    
    if ((addFollowIDMap.size() == 0) && (addFollowerIDMap.size() == 0)
        && (addTwitterIDMap.size() == 0)) System.out.println("新規追加IDはありません。");
  }

  public static void twitterRemoveUserIDCheck() throws TwitterException {
    Set<String> nowFollowIDSet = new HashSet<String>();
    Set<String> nowFollowerIDSet = new HashSet<String>();
    
    Set<String> oldFollowIDSet = new HashSet<String>();
    Set<String> oldFollowerIDSet = new HashSet<String>();
    
    Map<String,String> removeFollowIDMap = new HashMap<String,String>();
    Map<String,String> removeFollowerIDMap = new HashMap<String,String>();
    Map<String,String> removeTwitterIDMap = new HashMap<String,String>();
    
    getTwitterFollowIDList().forEach((v) -> {nowFollowIDSet.add(v);});
    getTwitterFollowerIDList().forEach((v) -> {nowFollowerIDSet.add(v);});
    
    sqlite.getTwitterIDList(1,1).forEach((v) -> {oldFollowIDSet.add(v);});
    sqlite.getTwitterIDList(2,1).forEach((v) -> {oldFollowerIDSet.add(v);}); 
    
    for(String id : oldFollowIDSet) {
      if(!nowFollowIDSet.contains(id)) removeFollowIDMap.put(id,"1");
    }
    for(String id : oldFollowerIDSet) {
      if(!nowFollowerIDSet.contains(id)) removeFollowerIDMap.put(id,"1");
    }
    
    //TwitterIDのRemoveFlgのチェック方法を見直す必要あり。2019/01/06
    //リムーブ・リフォローされたユーザーに対しsqlite内のNotFollow/RemoveFollowerをチェックしてどちらも1であればTwitterIDsのRemoveFlg=1にセットするロジックが必要
    for(String id : removeFollowIDMap.keySet()) {
      if(removeFollowerIDMap.get(id) != null) removeTwitterIDMap.put(id,"1");
    }
    
    if(removeFollowIDMap.size() != 0) {
      sqlite.updateTwitterID(removeFollowIDMap, 1);
      removeFollowIDMap.forEach((k,v) -> System.out.println(k));
    }
    if(removeFollowerIDMap.size() != 0) {
      sqlite.updateTwitterID(removeFollowerIDMap, 2);
      removeFollowerIDMap.forEach((k,v) -> System.out.println(k));
    }
    if(removeTwitterIDMap.size() != 0) {
      sqlite.updateTwitterID(removeTwitterIDMap, 0);
      removeTwitterIDMap.forEach((k,v) -> System.out.println(k));
    }

    if((removeFollowerIDMap.size() == 0) && (removeFollowIDMap.size() == 0)
        && (removeTwitterIDMap.size() == 0)) System.out.println("リムーブ更新対象のIDはありません。");
  }

  public static List<String> getTwitterFollowerIDList() throws TwitterException {
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

  public static List<String> getTwitterFollowIDList() throws TwitterException {
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


  public static void outputText(List<String> list) throws IOException {
    File file = new File("output.txt");
    PrintWriter printWriter = new PrintWriter(new BufferedWriter(
        new FileWriter(file)));
    for (String info : list) {
      printWriter.println(info);
    }
    printWriter.close();
  }

  public static String replaceStr(String str, String replace, String replacement) {
    String reg = replace;
    Pattern pat = Pattern.compile(reg);
    Matcher mat = pat.matcher(str);
    return mat.replaceAll(replacement);
  }

  public static String cutString(String str, String delimiter, int field) {
    int n = 0;
    for (String temp : str.split(delimiter)) {
      if (field == n)
        return temp;
      else
        n++;
    }
    return "";
  }

  public static String StringToUnicode(String str) {
    String unicodeStr = "";
    for (char temp : str.toCharArray()) {
      unicodeStr += "\\u" + Integer.toHexString((int) temp);
    }
    return unicodeStr;
  }

}
