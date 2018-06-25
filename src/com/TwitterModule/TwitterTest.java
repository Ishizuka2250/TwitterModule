package com.TwitterModule;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;
import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

//TwitterAPI制限 → 15分 
public class TwitterTest {
  private static String Consumer_key = "WOb0qHqJkttrUyYaBfFI2YNN7";
  private static String Consumer_secret = "ysjxfE5dzqYJyHPFdzBCYnLnrJvDr3g9FPgj60oTZwIZG7erBU";
  private static String Access_token = "2591693293-78onB1jTcVxq7ovrIOOEhSfKLa77AUmpITh02QP";
  private static String Access_token_secret = "bgV82VTVMsss7HAVWJg95YpiEiGEZelKmhlSEf413Q7pf";
  private static SqliteResource sqlite;
  
  public static void main(String args[]) throws TwitterException,IOException,ClassNotFoundException{   
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    sqlite = new SqliteResource();
    long start,end,followerIDsTime,followIDsTime,TwitterIDsTime;
    //outputText(getFollowerInfo());
    //sqlite.updateGetUserInfoFlg("3136594242", 0);
    //System.out.println(getUserInfo(twitter,"746272913968496641"));
    //sqlite.insertUserInfo("746272913968496641", "'746272913968496641','Zafarr Abbas','thezeenagari','Front-End Developer at Uconnect Pvt. Ltd.','14','1747','1108','22','Gilgit-Baltistan, Pakistan','Fri Jun 24 18:25:02 JST 2016','000000'");
    /*List<String> test = new ArrayList<String>();
    test.add("aaa");
    test.add("sss");
    HashMap<String,String> testMap = new HashMap<String,String>();
    test.forEach(s -> testMap.put(s, s));
    System.out.println(testMap.get("aaa"));*/
    
    //List<String> IDLists = getFollowerIDList();
    //sqlite.insertFollowerIDs(IDLists);
    //List<String> IDList = sqlite.getTwitterIDList(1);
    
    /*System.out.println("insert TwitterIDs");
    start = System.currentTimeMillis();
    List<String> FollowerIDList = getTwitterFollowerIDList();
    sqlite.insertTwitterIDs(FollowerIDList,1);
    end = System.currentTimeMillis();
    followerIDsTime = end - start;
    
    System.out.println("insert TwitterIDs");
    start = System.currentTimeMillis();
    List<String> FollowIDList = getTwitterFollowIDList();
    sqlite.insertTwitterIDs(FollowIDList,2);
    //getTwitterUserInfo(IDList);
    end = System.currentTimeMillis();
    followIDsTime = end - start;*/
    
    start = System.currentTimeMillis();
    List<String> TwitterAllIDs = sqlite.getTwitterIDAll();
    //List<String> TwitterAllIDs = new ArrayList<String>();
    //TwitterAllIDs.add("1287779750");
    //TwitterAllIDs.add("2739548718");
    getTwitterUserInfo(TwitterAllIDs);
    //sqlite.insertTwitterIDs(TwitterAllIDs, 0);
    //sqlite.updateFlgsOn(TwitterAllIDs);
    end = System.currentTimeMillis();
    TwitterIDsTime = end - start;
    System.out.println(TwitterIDsTime + "ms");
    
    /*
    start = System.currentTimeMillis();
    List<String> TwitterUpdateIDList = sqlite.getUpdateUserIDList();
    System.out.println("size:" + TwitterUpdateIDList.size());
    end = System.currentTimeMillis();
    TwitterIDsTime = end - start;*/
    
    //System.out.println("followerInsert:" + followerIDsTime + "\n" + "followinsert:" + followIDsTime + "\n" + "TwitterIDsinsert" + TwitterIDsTime);
    //System.out.println(TwitterIDsTime + "ms");
    System.out.println("done!");
  } 
 
  public static String inputString () {
    String input;
    BufferedReader buf = new BufferedReader(new InputStreamReader(System.in),1);
    try {
      System.out.print("tweet:");
      input = buf.readLine();
    }catch (IOException e) {
      System.out.println("error:IOException");
      return "";
    }
    return input;
  }
  
  public static ConfigurationBuilder twitterConfigure(){
    ConfigurationBuilder confbuilder = new ConfigurationBuilder();
    confbuilder.setDebugEnabled(true);
    confbuilder.setOAuthConsumerKey(Consumer_key);
    confbuilder.setOAuthConsumerSecret(Consumer_secret);
    confbuilder.setOAuthAccessToken(Access_token);
    confbuilder.setOAuthAccessTokenSecret(Access_token_secret);
    
    return confbuilder;
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
    }while(ids.hasNext());
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
    }while(ids.hasNext());
    return IDList;
  }
  
  //API制限までIDListのユーザー情報を取得する
  //API制限にてユーザー情報の取得できなかったユーザーはBufferTwitterIDsに格納される
  public static boolean getTwitterUserInfo(List<String> IDList) throws TwitterException {
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    Map<String, String> UserInfoMap = new HashMap<String, String>();
    List<String> APILimitUserList = new ArrayList<String>();
    Boolean APILimit = false; 
    String buffer="";
    long getUserInfoOK = 0L;
    long onHold = 0L;
    //hashmapを使うb
    for (String id : IDList) {
      if(!APILimit) {
        buffer = getUserInfo(twitter,id);
        if(!buffer.equals("-1")) {
          UserInfoMap.put(id, buffer);
          getUserInfoOK++;
        }else{
          APILimitUserList.add(id);
          onHold++;
          APILimit = true;
        }
      }else {
        APILimitUserList.add(id);
        onHold++;
      }
    }
    //updateのしょり
    System.out.println("ユーザー取得完了：" + getUserInfoOK + "\n保留：" + onHold);
    sqlite.insertBufferTwitterIDs(APILimitUserList);
    sqlite.updateUserInfo(UserInfoMap);
    
    if(buffer.equals("-1")) {
      System.out.println("API制限により処理を中断しました。");
      return false;
    }
    System.out.println("フォロワーIDの取得が完了しました。");
    return true;
  }
  
  //与えられたTwitterIDに該当するユーザー情報をカンマ区切りで返す
  public static String getUserInfo(Twitter twitter,String idStr) {
    long id = Long.parseLong(idStr);
    try{
      User user = twitter.showUser(id);
      String userName = replaceStr(user.getName(),"'","''");
      String userScreenName = replaceStr(user.getScreenName(),"'","''");
      String userDescription = replaceStr(user.getDescription(),"'","''");
      String userTweetCount = String.valueOf(user.getStatusesCount());
      String userFollowCount = String.valueOf(user.getFriendsCount());
      String userFollowerCount = String.valueOf(user.getFollowersCount());
      String userFavouritesCount = String.valueOf(user.getFavouritesCount());
      String userLocation = replaceStr(user.getLocation(),"'","''");
      String userCreatedAt = String.valueOf(user.getCreatedAt());
      String userBackGroundColor = String.valueOf(user.getProfileBackgroundColor());
      System.out.println("[" + userName + "]の情報を取得しました。 " + StringToUnicode(userName));
      return "'" + id + "','" + userName + "','" + userScreenName + "','" + userDescription + "','" + userTweetCount + "','" + userFollowCount + "','" + userFollowerCount + "','" + userFavouritesCount + "','" + userLocation + "','" + userCreatedAt + "','" + userBackGroundColor + "'";
    }catch (TwitterException te){
        if((te.getErrorCode() == 63) || (te.getErrorCode() == 50)) {
          System.out.println("[" + id + "] -> 凍結中のユーザーアカウントです。 ");
          return "1"; 
        }else if(te.getErrorCode() == 88) {
            //System.out.println("-> API制限に到達しました。5分間停止します。");
            return "-1";
        }else{
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

  public static void outputText(List<String> list) throws IOException {
    File file = new File("output.txt");
    PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(file)));
    for(String info : list) {
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

  public static String StringToUnicode(String str) {
    String unicodeStr = "";
    for(char temp : str.toCharArray()){
      unicodeStr += "\\u" + Integer.toHexString((int)temp);
    }
    return unicodeStr;
  }
  
}










