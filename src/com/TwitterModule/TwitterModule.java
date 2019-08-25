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
import com.TwitterModule.Twitter.TwitterUserInfo;

public class TwitterModule {
  private SqliteModule Sqlite;
  private TwitterCore TwitterCore;
  private long TwitterApiStopTimes = (15 * 60 * 1000) + (30 * 1000);// TwitterAPI���������15��
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);

  public TwitterModule(String UserName) throws ClassNotFoundException {
    TwitterCore = new TwitterCore(UserName);
    Sqlite = new SqliteModule(UserName);
  }
  
  /* 
   * TwitterIDs�ɕۑ�����Ă��郆�[�U�[�����X�V
   * FilterPattern = 0:TwitterIDs�ɓo�^����Ă���S���[�U�[���X�V
   * FilterPattern = 1:TwitterIDs�ɓo�^����Ă��郆�[�U�[���X�V (�t�H���[�E�t�H�����[�ł��Ȃ����l���[�U�[ ���� �A�J�E���g����������Ă��郆�[�U�[������)
   * FilterPattern = 2:TwitterIDs�ɓo�^����Ă���A�J�E���g����������Ă��郆�[�U�[���X�V
   * */
  public void selectUserInfoUpdate(int FilterPattern) throws TwitterException {
    List<String> IDList = new ArrayList<String>();
    if(FilterPattern == 0) IDList = Sqlite.getTwitterIDList(0,0);
    else if(FilterPattern == 1) IDList = Sqlite.getTwitterIDList(0,1);
    else if(FilterPattern == 2) IDList = Sqlite.getTwitterIDBan();
    else {
      System.out.println("Error:�����ȃt�B���^�[�p�^�[�������m����܂���.");
      System.exit(1);
    }
    Sqlite.updateFlgsOn(IDList);
    twitterUserInfoUpdate();
  }
  
  /*
   * TwitterIDs.UpdateFlg = 1 �̃��[�U�[�����X�V
   */
  public void twitterUserInfoUpdate() throws TwitterException {
    List<String> updateIDList = Sqlite.getUpdateUserIDList(1);

    // uploadFlg=1�̂��̂�����
    while (updateIDList.size() != 0) {
      if (getTwitterUserInfo(updateIDList) == true) {
        break;
      } else {
        System.out.println("Info:API�����ɂ�菈���𒆒f���܂���(" + TwitterApiStopTimes + "ms��~)");
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

  // API�����܂�IDList�̃��[�U�[�����擾����
  public boolean getTwitterUserInfo(List<String> IDList) throws TwitterException {
    Map<String, String> UserInfoMap = new HashMap<String, String>();
    Boolean APILimit = false;
    String buffer = "";
    long getUserInfoOK = 0L;
    long onHold = 0L;
    for (String id : IDList) {
      if (!APILimit) {
        buffer = getUserInfo(id);
        //getUserInfo����-1���Ԃ��ꂽ�ꍇAPI�����ɂ��ꎞ�I��User���擾�s��
        //-1 �ȊO�̏ꍇ�Ƀ}�b�v�փ��[�U�[�����i�[
        if (!buffer.equals("-1")) {
          UserInfoMap.put(id, buffer);
          getUserInfoOK++;
        //-1 �̏ꍇ, ���[�U��񖢎擾�̃��[�U�����J�E���g����
        } else {
          onHold++;
          APILimit = true;
        }
      } else {
        onHold++;
      }
    }
    //TwitterAPI�����ɂ�钆�f�E�S���[�U�[���擾��������Sqlite�փ��[�U�����i�[����
    Sqlite.updateUserInfo(UserInfoMap);
    System.out.println("Info:���[�U�[�擾���� -- " + getUserInfoOK + " �ۗ� -- " + onHold);
    
    //getUserInfo����-1���󂯎�����ꍇ���[�U�[�擾�����𒆒f����
    if (buffer.equals("-1")) {
      return false;
    }
    System.out.println("Info:�t�H�����[ID�̎擾���������܂���.");
    return true;
  }

  // TwitterID�ɊY�����郆�[�U�[�����擾���ASqlite�}���p��CSV�f�[�^���쐬����
  public String getUserInfo(String id) {
    try {
      TwitterUserInfo userInfo = new TwitterUserInfo(TwitterCore.getTwitterInstance().showUser(id));
      System.out.println("Info:[" + userInfo.UserName + "] �̏����擾���܂���. ");
      return userInfo.toString();
    } catch (TwitterException te) {
      if ((te.getErrorCode() == 63) || (te.getErrorCode() == 50)) {
        System.out.println("Info:[" + id + "] �������̃��[�U�[�A�J�E���g�ł�. ");
        return "1";
      } else if (te.getErrorCode() == 88) {
        System.out.println("Info:API�����ɓ��B���܂����B5���Ԓ�~���܂��B");
        return "-1";
      } else {
        te.printStackTrace(pw);
        pw.flush();
        System.out.println("Error:�A�J�E���g���擾���ɖ�肪�������܂���. \n" + StackTrace.toString());
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
    Sqlite.getTwitterIDList(0,0).forEach((v) -> {oldTwitterIDSet.add(v);});
    
    for (String id : nowTwitterIDSet) {
      //Sqlite�ɕۑ�����Ă��Ȃ��V�KID�̃`�F�b�N
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
      Sqlite.updateTwitterID(addFollowIDMap, 1);
      System.out.println("Info:TwitterFollowIDs -- " + addFollowIDMap.size() + "�� �ǉ�");
      //addFollowIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
    }
    if (addFollowerIDMap.size() != 0) {
      Sqlite.updateTwitterID(addFollowerIDMap, 2);
      System.out.println("Info:TwitterFollowerIDs -- " + addFollowerIDMap.size() + "�� �ǉ�");
      //addFollowerIDMap.forEach((k,v) -> {System.out.println("  " + k + ":" + v);});
    }
      
    if (addTwitterIDMap.size() != 0) {
      Sqlite.updateTwitterID(addTwitterIDMap, 0);
      System.out.println("Info:TwitterIDs update -- " + addTwitterIDMap.size() + "���@�ǉ�");
      Sqlite.updateFlgsOn(addTwitterIDList);
      twitterUserInfoUpdate();
    }
    
    if ((addFollowIDMap.size() == 0) && (addFollowerIDMap.size() == 0)
        && (addTwitterIDMap.size() == 0)) System.out.println("Info:�V�K�ǉ�ID�͂���܂���.");
  }

  public void twitterUpdateUserIDCheck() throws TwitterException {
    Set<String> nowFollowIDSet = new HashSet<String>();         //TwitterAPI����擾�����t�H���[���Ă��郆�[�UID
    Set<String> nowFollowerIDSet = new HashSet<String>();       //TwitterAPI����擾�����t�H���[����Ă��郆�[�UID
    Set<String> oldFollowIDSet = new HashSet<String>();         //DB����擾�����t�H���[���Ă��郆�[�UID
    Set<String> oldFollowerIDSet = new HashSet<String>();       //DB����擾�����t�H���[����Ă��郆�[�UID
    Set<String> oldRemoveFollowIDSet = new HashSet<String>();   //DB����擾�����t�H���[���Ă��Ȃ����[�UID
    Set<String> oldRemoveFollowerIDSet = new HashSet<String>(); //DB����擾�����t�H���[����O���ꂽ(�����[�u���ꂽ)���[�UID
    
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
    
    Sqlite.getTwitterIDList(1,1).forEach((v) -> {oldFollowIDSet.add(v);});
    Sqlite.getTwitterIDList(2,1).forEach((v) -> {oldFollowerIDSet.add(v);});
    Sqlite.getTwitterIDList(1,2).forEach((v) -> {oldRemoveFollowIDSet.add(v);});
    Sqlite.getTwitterIDList(2,2).forEach((v) -> {oldRemoveFollowerIDSet.add(v);});
    
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
    
    //�����[�u�E���t�H���[���ꂽ���[�U�[�ɑ΂�Sqlite����NotFollow/RemoveFollower���`�F�b�N���Ăǂ����1�ł����TwitterIDs��RemoveFlg=1�ɃZ�b�g����
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
    
    //Sqlite���X�V
    if(removeFollowIDMap.size() != 0) {
      Sqlite.updateTwitterID(removeFollowIDMap, 1);
      System.out.println("Info:�t�H���[���O�������[�U");
      removeFollowIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(removeFollowerIDMap.size() != 0) {
      Sqlite.updateTwitterID(removeFollowerIDMap, 2);
      System.out.println("Info:�t�H���[���O���ꂽ���[�U");
      removeFollowerIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(removeTwitterIDMap.size() != 0) {
      Sqlite.updateTwitterID(removeTwitterIDMap, 0);
      System.out.println("Info:�t�H���[���O���� & �t�H���[���O���ꂽ���[�U");
      removeTwitterIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    
    if(followIDMap.size() != 0) {
      Sqlite.updateTwitterID(followIDMap, 1);
      System.out.println("Info:�t�H���[�������[�U");
      followIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(followerIDMap.size() != 0) {
      Sqlite.updateTwitterID(followerIDMap, 2);
      System.out.println("Info:�t�H���[���ꂽ���[�U");
      followerIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    if(notRemoveTwitterIDMap.size() != 0) {
      Sqlite.updateTwitterID(notRemoveTwitterIDMap, 0);
      //System.out.println("Info:�ēx�t�H���[���� �������� �ēx�t�H���[���ꂽ���[�U");
      //notRemoveTwitterIDMap.forEach((k,v) -> System.out.println("  " + k));
    }
    
    int updateSize = removeFollowIDMap.size() + removeFollowerIDMap.size() + removeTwitterIDMap.size()
        + followIDMap.size() + followerIDMap.size() + notRemoveTwitterIDMap.size();
    if(updateSize == 0) System.out.println("Info:�����[�u�E�X�V�Ώۂ�ID�͂���܂���.");
  }

  public List<String> getTwitterFollowerIDList() throws TwitterException {
    long cursol = -1L;
    Twitter twitter = TwitterCore.getTwitterInstance();
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
    Twitter twitter = TwitterCore.getTwitterInstance();
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

}
