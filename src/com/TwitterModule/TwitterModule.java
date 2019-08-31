package com.TwitterModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import com.TwitterModule.Twitter.TwitterCore;
import com.TwitterModule.Twitter.TwitterIDSets;
import com.TwitterModule.Twitter.TwitterUserInfo;
import com.TwitterModule.SqliteModule.TwitterIDPattern;
import com.TwitterModule.SqliteModule.UserPattern;
import com.TwitterModule.SqliteModule.TableName;

public class TwitterModule {
  private SqliteModule Sqlite;
  private TwitterCore TwitterCore;
  private long TwitterApiStopTimes = (15 * 60 * 1000) + (30 * 1000);// TwitterAPI制限回避→15分
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);

  public TwitterModule(String UserName) throws ClassNotFoundException {
    TwitterCore = new TwitterCore(UserName);
    Sqlite = new SqliteModule(UserName);
  }
  
  /* 
   * TwitterIDsに保存されているユーザー情報を更新
   * FilterPattern = 0:TwitterIDsに登録されている全ユーザーを更新
   * FilterPattern = 1:TwitterIDsに登録されているユーザーを更新 (フォロー・フォロワーでもない他人ユーザー かつ アカウントが凍結されているユーザーを除く)
   * FilterPattern = 2:TwitterIDsに登録されているアカウントが凍結されているユーザーを更新
   * */
  public void selectUserInfoUpdate(int FilterPattern) throws TwitterException {
    List<String> IDList = new ArrayList<String>();
    if(FilterPattern == 0) IDList = Sqlite.getTwitterIDList(TwitterIDPattern.ALL_ID,UserPattern.ALL);
    else if(FilterPattern == 1) IDList = Sqlite.getTwitterIDList(TwitterIDPattern.ALL_ID,UserPattern.NO_REMOVE_USER_AND_BANUSER);
    else if(FilterPattern == 2) IDList = Sqlite.getTwitterIDBan();
    else {
      System.out.println("Error:無効なフィルターパターンが検知されました.");
      System.exit(1);
    }
    Sqlite.updateFlgsOn(IDList);
    twitterUserInfoUpdate();
  }
  
  /*
   * TwitterIDs.UpdateFlg = 1 のユーザー情報を更新
   */
  public void twitterUserInfoUpdate() throws TwitterException {
    List<String> updateIDList = Sqlite.getUpdateUserIDList(1);

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
        updateIDList = Sqlite.getUpdateUserIDList(1);
      }
    }
  }

  // API制限までIDListのユーザー情報を取得する
  public boolean getTwitterUserInfo(List<String> IDList) throws TwitterException {
    Map<String, String> UserInfoMap = new HashMap<String, String>();
    Boolean APILimit = false;
    String buffer = "";
    long getUserInfoOK = 0L;
    long onHold = 0L;
    for (String id : IDList) {
      if (!APILimit) {
        buffer = getUserInfo(id);
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
    Sqlite.updateUserInfo(UserInfoMap);
    System.out.println("Info:ユーザー取得完了 -- " + getUserInfoOK + " 保留 -- " + onHold);
    
    //getUserInfoから-1を受け取った場合ユーザー取得処理を中断する
    if (buffer.equals("-1")) {
      return false;
    }
    System.out.println("Info:フォロワーIDの取得が完了しました.");
    return true;
  }

  // TwitterIDに該当するユーザー情報を取得し、Sqlite挿入用のCSVデータを作成する
  public String getUserInfo(String id) {
    try {
      TwitterUserInfo userInfo = new TwitterUserInfo(TwitterCore.getTwitterInstance().showUser(Long.valueOf(id)));
      System.out.println("Info:[" + userInfo.UserName + "] の情報を取得しました. ");
      return userInfo.toString();
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
  
  public void twitterAddUserIDCheck() throws TwitterException {
    Boolean follow,follower;
    
    TwitterIDSets nowTwitterIDSets = new TwitterIDSets(TwitterCore.getTwitterInstance());
    TwitterIDSets oldTwitterIDSets = new TwitterIDSets(Sqlite);
    
    Map<String,String> addFollowIDMap = new HashMap<String,String>();
    Map<String,String> addFollowerIDMap = new HashMap<String,String>();
    Map<String, String> addTwitterIDMap = new HashMap<String, String>();
    List<String> addTwitterIDList = new ArrayList<String>();
    
    for (String id : nowTwitterIDSets.AllIDSet) {
      //Sqliteに保存されていない新規IDのチェック
      if(!oldTwitterIDSets.AllIDSet.contains(id)) {
        //0:フォローしている, 1:フォローしていない
        if(nowTwitterIDSets.FollowIDSet.contains(id)) {
          addFollowIDMap.put(id, "0");
          follow = true;
        }else{
          addFollowIDMap.put(id, "1");
          follow = false;
        }
        //0:フォローされている, 1:フォローされていない
        if(nowTwitterIDSets.FollowerIDSet.contains(id)){
          addFollowerIDMap.put(id, "0");
          follower = true;
        }else{
          addFollowerIDMap.put(id, "1");
          follower = false;
        }
        //フォローしていない かつ フォローされていない場合
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
      Sqlite.updateTwitterID(addFollowIDMap, TableName.TWITTER_FOLLOW_IDS);
      System.out.println("Info:TwitterFollowIDs -- " + addFollowIDMap.size() + "件 追加");
      //addFollowIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
    }
    if (addFollowerIDMap.size() != 0) {
      Sqlite.updateTwitterID(addFollowerIDMap, TableName.TWITTER_FOLLOWER_IDS);
      System.out.println("Info:TwitterFollowerIDs -- " + addFollowerIDMap.size() + "件 追加");
      //addFollowerIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
    }
      
    if (addTwitterIDMap.size() != 0) {
      Sqlite.updateTwitterID(addTwitterIDMap, TableName.TWITTER_IDS);
      System.out.println("Info:TwitterIDs update -- " + addTwitterIDMap.size() + "件　追加");
      Sqlite.updateFlgsOn(addTwitterIDList);
      twitterUserInfoUpdate();
    }
    
    if ((addFollowIDMap.size() == 0) && (addFollowerIDMap.size() == 0)
        && (addTwitterIDMap.size() == 0)) System.out.println("Info:新規追加IDはありません.");
  }

  public void twitterUpdateUserIDCheck() throws TwitterException {
    TwitterIDSets nowTwitterIDSets = new TwitterIDSets(TwitterCore.getTwitterInstance());
    TwitterIDSets oldTwitterIDSets = new TwitterIDSets(Sqlite);
    
    Map<String,String> followIDMap = new HashMap<String,String>();
    Map<String,String> followerIDMap = new HashMap<String,String>();
    Set<String> notRemoveTwitterIDSet = new HashSet<String>();
    Map<String,String> notRemoveTwitterIDMap = new HashMap<String,String>();
    Map<String,String> removeFollowIDMap = new HashMap<String,String>();
    Map<String,String> removeFollowerIDMap = new HashMap<String,String>();
    Set<String> removeTwitterIDSet = new HashSet<String>();
    Map<String,String> removeTwitterIDMap = new HashMap<String,String>();
    
    //過去時点でフォローしているユーザ と 現在フォローしているユーザ を突合 → フォローから外した
    for(String id : oldTwitterIDSets.FollowIDSet) {
      if(!nowTwitterIDSets.FollowIDSet.contains(id)) removeFollowIDMap.put(id,"1");
    }
    
    //現在フォローしているユーザ と 過去時点でフォローしているユーザ を突合 → フォローした
    for(String id : nowTwitterIDSets.FollowIDSet) {
      if(!oldTwitterIDSets.FollowIDSet.contains(id)) followIDMap.put(id,"0");
    }
    
    //過去時点でフォローされているユーザ と 現在でフォローされているユーザ を突合 → フォローから外された
    for(String id : oldTwitterIDSets.FollowerIDSet) {
      if(!nowTwitterIDSets.FollowerIDSet.contains(id)) removeFollowerIDMap.put(id,"1");
    }
    
    //現在フォローされているユーザ と 過去時点でフォローされているユーザを突合 → フォローされた
    for(String id : nowTwitterIDSets.FollowerIDSet) {
      if(!oldTwitterIDSets.FollowerIDSet.contains(id)) followerIDMap.put(id,"0");
    }
    
    //リムーブ・リフォローされたユーザーに対しSqlite内のNotFollow/RemoveFollowerをチェックしてどちらも1であればTwitterIDsのRemoveFlg=1にセットする
    for(String id : removeFollowIDMap.keySet()) {
      if((removeFollowerIDMap.get(id) != null) || (oldTwitterIDSets.RemoveFollowerIDSet.contains(id))) {
        removeTwitterIDSet.add(id);
      }
    }
    for(String id : removeFollowerIDMap.keySet()) {
      if((removeFollowIDMap.get(id) != null) || (oldTwitterIDSets.RemoveFollowIDSet.contains(id))) {
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
      Sqlite.updateTwitterID(removeFollowIDMap, TableName.TWITTER_FOLLOW_IDS);
      System.out.println("Info:フォローを外したユーザ");
      removeFollowIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(removeFollowerIDMap.size() != 0) {
      Sqlite.updateTwitterID(removeFollowerIDMap, TableName.TWITTER_FOLLOWER_IDS);
      System.out.println("Info:フォローを外されたユーザ");
      removeFollowerIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(removeTwitterIDMap.size() != 0) {
      Sqlite.updateTwitterID(removeTwitterIDMap, TableName.TWITTER_IDS);
      //System.out.println("Info:フォローを外した もしくは フォローを外されたユーザ");
      //removeTwitterIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    
    if(followIDMap.size() != 0) {
      Sqlite.updateTwitterID(followIDMap, TableName.TWITTER_FOLLOW_IDS);
      System.out.println("Info:フォローしたユーザ");
      followIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(followerIDMap.size() != 0) {
      Sqlite.updateTwitterID(followerIDMap, TableName.TWITTER_FOLLOWER_IDS);
      System.out.println("Info:フォローされたユーザ");
      followerIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(notRemoveTwitterIDMap.size() != 0) {
      Sqlite.updateTwitterID(notRemoveTwitterIDMap, TableName.TWITTER_IDS);
      //System.out.println("Info:再度フォローした もしくは 再度フォローされたユーザ");
      //notRemoveTwitterIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    
    int updateSize = removeFollowIDMap.size() + removeFollowerIDMap.size() + removeTwitterIDMap.size()
        + followIDMap.size() + followerIDMap.size() + notRemoveTwitterIDMap.size();
    if(updateSize == 0) System.out.println("Info:更新対象のユーザはいません.");
  }



}
