package com.TwitterModule;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;

import twitter4j.*;
import twitter4j.auth.*;
import twitter4j.conf.*;

public class TwitterModule {
  private static String Consumer_key = "WOb0qHqJkttrUyYaBfFI2YNN7";
  private static String Consumer_secret = "ysjxfE5dzqYJyHPFdzBCYnLnrJvDr3g9FPgj60oTZwIZG7erBU";
  private static String Access_token = "2591693293-78onB1jTcVxq7ovrIOOEhSfKLa77AUmpITh02QP";
  private static String Access_token_secret = "bgV82VTVMsss7HAVWJg95YpiEiGEZelKmhlSEf413Q7pf";
  private static SqliteResource sqlite;
  private static long TwitterApiStopTimes = (15 * 60 * 1000) + (30 * 1000);// TwitterAPI������15��

  public static void main(String args[]) throws TwitterException, IOException,ClassNotFoundException {
    sqlite = new SqliteResource();
    long start, end, TwitterIDsTime;

    start = System.currentTimeMillis();
    //twitterUserInfoUpdate();
    //twitterAddUserIDCheck();
    twitterRemoveUserIDCheck();
    end = System.currentTimeMillis();
    TwitterIDsTime = end - start;
    System.out.println(TwitterIDsTime + "ms");
    System.out.println("done!");
  }

  public static ConfigurationBuilder twitterConfigure() {
    ConfigurationBuilder confbuilder = new ConfigurationBuilder();
    confbuilder.setDebugEnabled(true);
    confbuilder.setOAuthConsumerKey(Consumer_key);
    confbuilder.setOAuthConsumerSecret(Consumer_secret);
    confbuilder.setOAuthAccessToken(Access_token);
    confbuilder.setOAuthAccessTokenSecret(Access_token_secret);
    return confbuilder;
  }

  public static void twitterUserInfoUpdateAll() throws TwitterException {
    List<String> allIDList = sqlite.getTwitterIDAll(true);
    sqlite.updateFlgsOn(allIDList);
    twitterUserInfoUpdate();
  }

  public static void twitterUserInfoUpdate() throws TwitterException {
    List<String> updateIDList = sqlite.getUpdateUserIDList(1);

    // uploadFlg=1�̂��̂�����
    while (updateIDList.size() != 0) {
      if (getTwitterUserInfo(updateIDList) == true) {
        break;
      } else {
        System.out.println("API�����ɂ�菈���𒆒f���܂����B\n-> " + TwitterApiStopTimes + "ms��~");
        try {
          Thread.sleep(TwitterApiStopTimes);
        } catch (InterruptedException e) {}
        updateIDList = sqlite.getUpdateUserIDList(1);
      }
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

  public static void twitterRemoveUserIDCheck() throws TwitterException {
    Map<String,String> nowFollowIDMap = new HashMap<String,String>();
    Map<String,String> nowFollowerIDMap = new HashMap<String,String>();
    Map<String,String> nowTwitterIDMap = new HashMap<String,String>();
    List<String> oldFollowIDList = new ArrayList<String>();
    List<String> oldFollowerIDList = new ArrayList<String>();
    List<String> oldTwitterIDList = new ArrayList<String>();
    List<String> removeFollowIDList = new ArrayList<String>();
    List<String> removeFollowerIDList = new ArrayList<String>();
    List<String> removeTwitterIDList = new ArrayList<String>();
    
    getTwitterFollowerIDList().forEach(s -> nowFollowerIDMap.put(s, s));
    getTwitterFollowIDList().forEach(s -> nowFollowIDMap.put(s, s));
    oldTwitterIDList = sqlite.getTwitterIDList(0);
    oldFollowerIDList = sqlite.getTwitterIDList(1);
    oldFollowIDList = sqlite.getTwitterIDList(2);
    
    for (String id : oldFollowerIDList) {
      if(nowFollowerIDMap.get(id) == null) removeFollowerIDList.add(id);
      nowTwitterIDMap.put(id, id);
    }
    for (String id : oldFollowIDList) {
      if(nowFollowIDMap.get(id) == null) removeFollowIDList.add(id);
      if(nowTwitterIDMap.get(id) == null) nowTwitterIDMap.put(id, id);
    }
    for(String id : oldTwitterIDList) {
      if(nowTwitterIDMap.get(id) ==null) removeTwitterIDList.add(id);
    }
    
    if(removeFollowerIDList.size() != 0) {
      //removeFollowerIDList.forEach(s -> System.out.println(s));
      sqlite.updateRemoveFlgs(removeFollowerIDList, 1);
      System.out.println("removeFollower Update -- oK");
    }
    if(removeFollowIDList.size() != 0) {
      //removeFollowIDList.forEach(s -> System.out.println(s));
      sqlite.updateRemoveFlgs(removeFollowIDList, 2);
      System.out.println("removeFollow update -- ok");
    }
    if(removeTwitterIDList.size() != 0) {
      //removeTwitterIDList.forEach(s -> System.out.println(s));
      sqlite.updateRemoveFlgs(removeTwitterIDList, 0);
      System.out.println("removeTwitterID update -- ok");
    }
    if((removeFollowerIDList.size() == 0) && (removeFollowIDList.size() == 0)
        && (removeTwitterIDList.size() == 0)) System.out.println("�����[�u�X�V�Ώۂ�ID�͂���܂���B");
  }

  public static void twitterAddUserIDCheck() throws TwitterException {
    List<String> nowFollowIDList = new ArrayList<String>();
    List<String> nowFollowerIDList = new ArrayList<String>();
    Map<String, String> oldFollowIDMap = new HashMap<String, String>();
    Map<String, String> oldFollowerIDMap = new HashMap<String, String>();
    Map<String, String> oldTwitterIDMap = new HashMap<String, String>();
    List<String> addFollowIDList = new ArrayList<String>();
    List<String> addFollowerIDList = new ArrayList<String>();
    Map<String, String> addTwitterIDMap = new HashMap<String, String>();
    List<String> addTwitterIDList = new ArrayList<String>();

    nowFollowIDList = getTwitterFollowIDList();
    nowFollowerIDList = getTwitterFollowerIDList();
    sqlite.getTwitterIDList(0).forEach(s -> oldTwitterIDMap.put(s, s));
    sqlite.getTwitterIDList(1).forEach(s -> oldFollowerIDMap.put(s, s));
    sqlite.getTwitterIDList(2).forEach(s -> oldFollowIDMap.put(s, s));

    for (String id : nowFollowerIDList) {
      if (oldFollowerIDMap.get(id) == null) {
        addFollowerIDList.add(id);
        if (oldTwitterIDMap.get(id) == null) {
          addTwitterIDMap.put(id, "follower");
        }
      }
    }
    for (String id : nowFollowIDList) {
      if (oldFollowIDMap.get(id) == null) {
        addFollowIDList.add(id);
        if ((oldTwitterIDMap.get(id) == null) && (addTwitterIDMap.get(id) == null)) {
          addTwitterIDMap.put(id, "follow");
        }
      }
    }
    addTwitterIDMap.forEach((k, v) -> addTwitterIDList.add(k));

    if (addFollowerIDList.size() != 0) sqlite.insertTwitterIDs(addFollowerIDList, 1);
    if (addFollowIDList.size() != 0) sqlite.insertTwitterIDs(addFollowIDList, 2);
    if (addTwitterIDList.size() != 0) {
      sqlite.insertTwitterIDs(addTwitterIDList, 0);
      sqlite.updateFlgsOn(addTwitterIDList);
      twitterUserInfoUpdate();
    }
    if ((addFollowIDList.size() == 0) && (addFollowerIDList.size() == 0)
        && (addTwitterIDList.size() == 0)) System.out.println("�V�K�ǉ�ID�͂���܂���B");
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

  // API�����܂�IDList�̃��[�U�[�����擾����
  public static boolean getTwitterUserInfo(List<String> IDList) throws TwitterException {
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    Map<String, String> UserInfoMap = new HashMap<String, String>();
    List<String> OnHoldUserList = new ArrayList<String>();
    Boolean APILimit = false;
    String buffer = "";
    long getUserInfoOK = 0L;
    long onHold = 0L;
    // hashmap���g��b
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
    // update�̂����
    System.out.println("���[�U�[�擾�����F" + getUserInfoOK + "\n�ۗ��F" + onHold);
    // sqlite.insertBufferTwitterIDs(OnHoldUserList);
    sqlite.updateUserInfo(UserInfoMap);

    if (buffer.equals("-1")) {
      return false;
    }
    System.out.println("�t�H�����[ID�̎擾���������܂����B");
    return true;
  }

  // �^����ꂽTwitterID�ɊY�����郆�[�U�[�����J���}��؂�ŕԂ�
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
      System.out.println("[" + userName + "]�̏����擾���܂����B " + StringToUnicode(userName));
      return "'" + id + "','" + userName + "','" + userScreenName + "','"
          + userDescription + "','" + userTweetCount + "','" + userFollowCount
          + "','" + userFollowerCount + "','" + userFavouritesCount + "','"
          + userLocation + "','" + userCreatedAt + "','" + userBackGroundColor + "'";
    } catch (TwitterException te) {
      if ((te.getErrorCode() == 63) || (te.getErrorCode() == 50)) {
        System.out.println("[" + id + "] -> �������̃��[�U�[�A�J�E���g�ł��B ");
        return "BAN";
      } else if (te.getErrorCode() == 88) {
        // System.out.println("-> API�����ɓ��B���܂����B5���Ԓ�~���܂��B");
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
