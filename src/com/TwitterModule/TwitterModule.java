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
  private static long TwitterApiStopTimes = (15 * 60 * 1000) + (30 * 1000);// TwitterAPI���������15��

  public static void main(String args[]) throws TwitterException, IOException,ClassNotFoundException {
    long start, end, TwitterIDsTime;
    initTwitterModule();
    TwitterFactory twitterFactory = new TwitterFactory(twitterConfigure().build());
    Twitter twitter = twitterFactory.getInstance();
    
    start = System.currentTimeMillis();
    //�V�K�ǉ����[�U�[�̃`�F�b�N
    //twitterAddUserIDCheck();
    //�t�H���[�E���t�H���[�̃`�F�b�N
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
   * TwitterIDs�ɕۑ�����Ă��郆�[�U�[�����X�V
   * FilterPattern = 0:TwitterIDs�ɓo�^����Ă���S���[�U�[���X�V
   * FilterPattern = 1:TwitterIDs�ɓo�^����Ă��郆�[�U�[���X�V (�t�H���[�E�t�H�����[�ł��Ȃ����l���[�U�[ ���� �A�J�E���g����������Ă��郆�[�U�[������)
   * FilterPattern = 2:TwitterIDs�ɓo�^����Ă���A�J�E���g����������Ă��郆�[�U�[���X�V
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
   * TwitterIDs.UpdateFlg = 1 �̃��[�U�[�����X�V
   */
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
      System.out.println(id + ":[" + userName + "]�̏����擾���܂����B ");
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

    //TwitterAPI��茻�݃t�H���[���Ă��郆�[�U�[���擾
    getTwitterFollowIDList().forEach((v) -> {
        nowFollowIDSet.add(v);
        nowTwitterIDSet.add(v);
    });
    //TwitterAPI��茻�݃t�H���[����Ă��郆�[�U�[���擾
    getTwitterFollowerIDList().forEach((v) -> {
        nowFollowerIDSet.add(v);
        nowTwitterIDSet.add(v);
    });
    
    //TwitterIDs�ɓo�^����Ă���STwitterID���擾����
    sqlite.getTwitterIDList(0,0).forEach((v) -> {oldTwitterIDSet.add(v);});
    
    for (String id : nowTwitterIDSet) {
      //������ID���ɑ��݂��Ȃ��V�KID�̃`�F�b�N
      if(!oldTwitterIDSet.contains(id)) {
        //0:�t�H���[, 1:�t�H���[���Ă��Ȃ�
        if(nowFollowIDSet.contains(id)) {
          addFollowIDMap.put(id, "0");
          follow = true;
        }else{
          addFollowIDMap.put(id, "1");
          follow = false;
        }
        //0:�t�H�����[, 1:�t�H�����[�ł͂Ȃ� 
        if(nowFollowerIDSet.contains(id)){
          addFollowerIDMap.put(id, "0");
          follower = true;
        }else{
          addFollowerIDMap.put(id, "1");
          follower = false;
        }
        //�t�H���[���Ă��Ȃ� ���� �t�H�����[�ł��Ȃ��ꍇ
        if((!follow) && (!follower)) {
          addTwitterIDMap.put(id, "1");
        }else{
          addTwitterIDMap.put(id, "0");
        }
      }
    }
    addTwitterIDMap.forEach((k,v) -> {addTwitterIDList.add(k);});

    //Twitter�Ŏ擾�����eID����DB(Sqlite)�֕ۑ�
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
        && (addTwitterIDMap.size() == 0)) System.out.println("�V�K�ǉ�ID�͂���܂���B");
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
    
    //TwitterID��RemoveFlg�̃`�F�b�N���@���������K�v����B2019/01/06
    //�����[�u�E���t�H���[���ꂽ���[�U�[�ɑ΂�sqlite����NotFollow/RemoveFollower���`�F�b�N���Ăǂ����1�ł����TwitterIDs��RemoveFlg=1�ɃZ�b�g���郍�W�b�N���K�v
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
        && (removeTwitterIDMap.size() == 0)) System.out.println("�����[�u�X�V�Ώۂ�ID�͂���܂���B");
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
