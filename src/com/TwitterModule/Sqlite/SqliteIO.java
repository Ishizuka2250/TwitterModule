package com.TwitterModule.Sqlite;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.TwitterModule.Twitter.TwitterIO.FollowState;

public class SqliteIO {
  private String SqliteDirPath;
  private String SqlitePath;
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  public enum TableName {
    TWITTER_IDS,
    TWITTER_FOLLOW_IDS,
    TWITTER_FOLLOWER_IDS
  }
  
  public enum TwitterIDPattern {
    /** �SUserID(Follow_ID + Follower_ID)���w�� */
    ALL_ID,
    /** �t�H���[�������[�U�̂ݎw�� */
    FOLLOW_ID,
    /** �t�H���[����Ă��郆�[�U�̂ݎw�� */
    FOLLOWER_ID
  }
  
  public enum UserPattern {
    /** ���ׂẴ��[�U(�����w�薳��) */
    ALL,
    /** �t�H���[���Ă��� �������� �t�H���[����Ă��郆�[�U��Ώ� */
    NO_REMOVE_USER_AND_BANUSER,
    /** �t�H���[���Ă��Ȃ� �������� �t�H���[����Ă��Ȃ����[�U, ��������Ă��郆�[�U��Ώ� */
    REMOVE_USER_AND_BANUSER
  }
  
  public SqliteIO(String UserName) throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    SqliteDirPath = "D:/twitterApp";
    SqlitePath = SqliteDirPath +  "/" + UserName + ".sqlite";
    InitSqlite initSqlite = new InitSqlite(UserName,SqlitePath);
    initSqlite.tableCheck();
  }
  
  //sqlite��pragma�ݒ�
  //https://qiita.com/Khromium/items/c333ddebfe81ed4e35f7
  public static Properties getProperties() {
    Properties property = new Properties();
    property.put("journal_mode", "MEMORY");
    property.put("sync_mode", "OFF");
    property.put("autocommit", "OFF");
    return property;
  }
  
  private void outputSQLStackTrace(SQLException e,String SQL) {
    e.printStackTrace(pw);
    pw.flush();
    System.out.println("Error:�N�G���̎��s�Ɏ��s���܂���.");
    System.out.println(StackTrace.toString());
    System.out.println("--Missing SQL------------------------");
    System.out.println(SQL);
    System.out.println("-------------------------------------");
    System.exit(1);
  }
  
  //Map<TwitterID,User�̏��[0:�������t�H���[���� �������� ���肩��t�H���[���ꂽ���, 1:�������t�H���[���O���� �������� ���肩��t�H���[���O���ꂽ���]>
  //insertPattern = 0 TwitterIDs�e�[�u���̍X�V
  //insertPattern = 1 TwitterfollowIDs�e�[�u���̍X�V
  //insertPattern = 2 TwitterFollowerIDs�e�[�u���̍X�V
  public void updateTwitterID(Map<String, FollowState> IDMap, TableName updateTableName) {
    String SQL = "";
    String executeType = "";
    long recordCount;
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath, getProperties());
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      statement.execute("begin transaction;");
      
      //TwitterIDs/TwitterFollowIDs/TwitterFollowerIDs : �������R�[�h�̃`�F�b�N
      for(String id : IDMap.keySet()) {
        switch (updateTableName) {
          case TWITTER_IDS:
            SQL = "select Count(*) from TwitterIDs\n"
                + "where TwitterID = '" + id + "'";
            break;
          
          case TWITTER_FOLLOW_IDS:
            SQL = "select Count(*) from TwitterFollowIDs\n"
                + "where TwitterID = '" + id + "'";
            break;
            
          case TWITTER_FOLLOWER_IDS:
            SQL = "select Count(*) from TwitterFollowerIDs\n"
                + "where TwitterID = '" + id + "'";
            break;
        }
        
        ResultSet result = statement.executeQuery(SQL);
        recordCount = result.getInt(1);
        
        //TwitterIDs TwitterFollowIDs TwitterFollowerIDs : ���R�[�h�V�K�ǉ���(recordCount = 0)
        if(recordCount == 0) {
          executeType = "�V�K�ǉ�";
          switch (updateTableName) {
            case TWITTER_IDS:
              //NOT_REMOVE�̏ꍇ:RemoveFlg = 0, REMOVE�̏ꍇ:RemoveFlg = 1
              if(IDMap.get(id) == FollowState.NOT_REMOVE) {
                statement.execute("insert into TwitterIDs values('" + id + "', 0, 0, '" + sdf.format(cal.getTime()) + "', '000000000000', '0', '0', '0')");
              }else if(IDMap.get(id) == FollowState.REMOVE){
                statement.execute("insert into TwitterIDs values('" + id + "', 0, 0, '" + sdf.format(cal.getTime()) + "', '000000000000', '0', '1', '0')");
              }break;
            
            case TWITTER_FOLLOW_IDS:
              //id�̏�Ԃ�"0"�̏ꍇ:NotFollowFlg = 0, id�̏�Ԃ�"1"�̏ꍇ:NotFollowFlg = 1
              if(IDMap.get(id) == FollowState.FOLLOW) {
                statement.execute("insert into TwitterFollowIDs values('" + id + "', '0', '0','" + sdf.format(cal.getTime()) + "');");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOW){
                statement.execute("insert into TwitterFollowIDs values('" + id + "', '1', '0','" + sdf.format(cal.getTime()) + "');");
              }break;
              
            case TWITTER_FOLLOWER_IDS:
              //id�̏�Ԃ�"0"�̏ꍇ:RemoveFollowFlg = 0, id�̏�Ԃ�"1"�̏ꍇ:RemoveFollowFlg = 1
              if(IDMap.get(id) == FollowState.FOLLOWER) {
                statement.execute("insert into TwitterFollowerIDs values('" + id + "', '0','" + sdf.format(cal.getTime()) + "');");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOWER){
                statement.execute("insert into TwitterFollowerIDs values('" + id + "', '1','" + sdf.format(cal.getTime()) + "');");
              }break;
          }
        }else{
          //TwitterIDs TwitterFollowIDs TwitterFollowerIDs : �������R�[�h�̍X�V (recordCount = 1)
          executeType = "�X�V";
          //TwitterIDs�e�[�u���̍X�V
          switch (updateTableName) {
            case TWITTER_IDS:
              if(IDMap.get(id) == FollowState.NOT_REMOVE) {
                statement.execute("update TwitterIDs set RemoveFlg = 0 where TwitterID = '" + id + "';");
              }else if(IDMap.get(id) == FollowState.REMOVE) {
                statement.execute("update TwitterIDs set RemoveFlg = 1 where TwitterID = '" + id + "';");
              }
              statement.execute("update TwitterIDs set UpdateTime = '" + sdf.format(cal.getTime()) + "' where TwitterID = '" + id + "';");
              break;
              
            case TWITTER_FOLLOW_IDS:
              if(IDMap.get(id) == FollowState.FOLLOW) {
                statement.execute("update TwitterFollowIDs set NotFollowFlg = 0 where TwitterID = '" + id + "';");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOW) {
                statement.execute("update TwitterFollowIDs set NotFollowFlg = 1 where TwitterID = '" + id + "';");
              }
              statement.execute("update TwitterFollowIDs set UpdateTime = '" + sdf.format(cal.getTime()) + "' where TwitterID = '" + id + "';");
              break;
              
            case TWITTER_FOLLOWER_IDS:
              if(IDMap.get(id) == FollowState.FOLLOWER) {
                statement.execute("update TwitterFollowerIDs set RemoveFollowerFlg = 0 where TwitterID = '" + id + "';");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOWER) {
                statement.execute("update TwitterFollowerIDs set RemoveFollowerFlg = 1 where TwitterID = '" + id + "';");
              }
              statement.execute("update TwitterFollowerIDs set UpdateTime = '" + sdf.format(cal.getTime()) + "' where TwitterID = '" + id + "';");
              break;
          }
        }
      }
      statement.execute("commit;");
      statement.close();
      connection.close();
      if(updateTableName == TableName.TWITTER_IDS) System.out.println("Info:squite.TwitterIDs " + executeType + " -- OK.");
      else if(updateTableName == TableName.TWITTER_FOLLOW_IDS) System.out.println("Info:sqlite.TwitterFollowIDs " + executeType + " -- OK.");
      else if(updateTableName == TableName.TWITTER_FOLLOWER_IDS) System.out.println("Info:sqlite.TwitterFollowerIDs " + executeType + " -- OK.");
    }catch (SQLException e) {
      outputSQLStackTrace(e,SQL);
    }
  }
  
  public void updateFlgsOn(List<String> IDList) {
    String SQL = "";
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath,getProperties());
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      statement.execute("begin transaction");
      for(String temp : IDList) {
        SQL = "update TwitterIDs set UpdateFlg = '1'\n"
            + "where TwitterID = '" + temp + "'";
        statement.execute(SQL);
      }
      statement.execute("commit");
      statement.close();
      connection.close();
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
  }
  
  public List<String> getUpdateUserIDList(int UpdateFlg) {
    List<String> TwitterIDList = new ArrayList<String>();
    String SQL = "";
    try{
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath,getProperties());
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      SQL = "select TwitterID from TwitterIDs\n"
          + "where UpdateFlg = '" + UpdateFlg + "'";
      ResultSet result = statement.executeQuery(SQL);
      while(result.next()) {
        TwitterIDList.add(result.getString(1));
      }
    }catch (SQLException e) {
      outputSQLStackTrace(e,SQL);
    }
    return TwitterIDList;
  }
  
  public boolean updateUserInfo(Map<String, String> UserIDMap) {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    
    String SQL = "";
    try{
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath, getProperties());
      Statement statement = connection.createStatement();
      ResultSet result,banCountResult;
      int updateUserInfoTimes;
      statement.setQueryTimeout(30);
      statement.execute("begin transaction");
      for(String temp : UserIDMap.keySet()) {
        SQL = "select count(*) from TwitterUserInfo\n"
            + "where TwitterID = '" + temp + "'";
        result = statement.executeQuery(SQL);
        result.next();
        if(result.getInt(1) == 0) {
          //TwitterUserInfo�V�K�o�^��
          //�A�JBAN����Ă���UserID���ǂ����̔���[UserIDMap.get(temp) = "1" �� �A�JBan] 
          if(UserIDMap.get(temp).equals("1") == false) {//���[�U�[��񐳏�擾��(Not�A�JBAN)
            SQL = "insert into TwitterUserInfo values(" + UserIDMap.get(temp) + ");";
            statement.execute(SQL);
            SQL = "update TwitterIDs set UpdateUserInfoTimes = 1,UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0', BanUserFlg = '0'\n"
                + "where TwitterID = '" + temp + "'";
          }else{//�A�JBan
            //TwitterIDs���X�V�񐔂��擾
            SQL = "select * from TwitterIDs\n"
                + "where TwitterID = '" + temp + "'";
            banCountResult = statement.executeQuery(SQL);
            banCountResult.next();
            updateUserInfoTimes = banCountResult.getInt(2);
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0', BanUserFlg = '1'\n"
                + "where TwitterID = '" + temp + "'";
          }
          statement.execute(SQL);
        }else{//���[�U�[���o�^�ς� (�������R�[�h�̕����폜 �� �X�V���R�[�h�}��)
          //�ߋ��f�[�^�̕����폜
          SQL = "delete from TwitterUserInfo\n"
              + "where TwitterID = '" + temp + "'";
          statement.execute(SQL);
          //TwitterIDs���X�V�񐔂��擾
          SQL = "select * from TwitterIDs\n"
              + "where TwitterID = '" + temp + "'";
          result = statement.executeQuery(SQL);
          result.next();
          updateUserInfoTimes = result.getInt(2);
          //�A�JBAN����Ă��郆�[�U���ǂ�������. [UserIDMap.get(temp) = "1" �� �A�JBan����Ă��郆�[�U]
          if(UserIDMap.get(temp).equals("1") == false) {//���[�U�[��񐳏�擾��
            //User���X�V
            SQL = "insert into TwitterUserInfo values(" + UserIDMap.get(temp) + ");";
            statement.execute(SQL);
            
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0'\n"
                + "where TwitterID = '" + temp + "'";
          }else{//�A�JBan
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0', BanUserFlg = '1'\n"
                + "where TwitterID = '" + temp + "'";
          }
          statement.execute(SQL);
          result.close();
        }
      }
      statement.execute("commit");
      
      statement.close();
      connection.close();
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
    return true;
  }

  //UserListFlg = 0 twitterIDs
  //UserListFlg = 1 follow
  //UserListFlg = 2 follower
  //RemoveUser = 0
  //  TwitterIDs:TwitterIDs�ɓo�^����Ă���ID�����ׂĎ擾����B
  //  TwitterFollowIDs:TwitterFollowIDs�ɓo�^����Ă���ID�����ׂĎ擾����B
  //  TwitterFollowerIDs:TwitterFollowerIDs�ɓo�^����Ă���ID�����ׂĎ擾����B
  //RemoveUser = 1
  //  TwitterIDs:�t�H���[���t�H���o������Ă��Ȃ����[�U�[ ���� ��������Ă��郆�[�U�[ �͏��O����B
  //  TwitterFollowIDs:���t�H���[���Ă��郆�[�U�[ ���� ��������Ă��郆�[�U�[ �͏��O����B
  //  TwitterFollowerIDs:�t�H���o����Ă��Ȃ����[�U�[ ���� ��������Ă��郆�[�U�[ �͏��O����B
  //RemoveUser = 2
  //  TwitterIDs:�t�H���[���t�H���o������Ă��Ȃ����[�U�[ �� ��������Ă��郆�[�U�[ ���擾����B
  //  TwitterFollowIDs:���t�H���[���Ă��郆�[�U�[ �� ��������Ă��郆�[�U�[ ���擾����B
  //  TwitterFollowerIDs:�t�H���o����Ă��Ȃ����[�U�[ �� ��������Ă��郆�[�U�[ ���擾����B
  public List<String> getTwitterIDList(TwitterIDPattern twitterIDPattern, UserPattern userPattern) {
    List<String> userList = new ArrayList<String>();
    String SQL="";
    int banUserFlg,removeFlg,notFollowFlg,removeFollowerFlg;
    
    if((userPattern == UserPattern.ALL) || (userPattern == UserPattern.NO_REMOVE_USER_AND_BANUSER)) {
      banUserFlg = removeFlg = notFollowFlg = removeFollowerFlg = 0;
    }else{
      banUserFlg = removeFlg = notFollowFlg = removeFollowerFlg = 1;
    }
    
    try{
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      switch (twitterIDPattern) {
        case ALL_ID:
          if(userPattern == UserPattern.ALL) {
            SQL = "select TwitterIDs.TwitterID from TwitterIDs";
          }else{
            SQL = "select TwitterIDs.TwitterID from TwitterIDs\n"
                + "where TwitterIDs.RemoveFlg = " + removeFlg + "\n";
            if(userPattern == UserPattern.NO_REMOVE_USER_AND_BANUSER) {
              SQL = SQL + "and TwitterIDs.BanUserFlg = " + banUserFlg + ";";
            }else if(userPattern == UserPattern.REMOVE_USER_AND_BANUSER){
              SQL = SQL + "or TwitterIDs.BanUserFlg = " + banUserFlg + ";";
            }
          }break;
        
        case FOLLOW_ID:
          if(userPattern == UserPattern.ALL) {
            SQL = "select TwitterFollowIDs.TwitterID from TwitterFollowIDs";
          }else{
            SQL = "select TwitterFollowIDs.TwitterID from TwitterFollowIDs\n"
                + "left outer join TwitterIDs\n"
                + "on TwitterFollowIDs.TwitterID = TwitterIDs.TwitterID\n"
                + "where TwitterFollowIDs.NotFollowFlg = " + notFollowFlg + "\n";
            if(userPattern == UserPattern.NO_REMOVE_USER_AND_BANUSER) {
              SQL = SQL + "and TwitterIDs.BanUserFlg = " + banUserFlg + ";";
            }else if(userPattern == UserPattern.REMOVE_USER_AND_BANUSER){
              SQL = SQL + "or TwitterIDs.BanUserFlg = " + banUserFlg + ";";
            }
          }break;
        
        case FOLLOWER_ID:
          if(userPattern == UserPattern.ALL) {
            SQL = "select TwitterFollowerIDs.TwitterID from TwitterFollowerIDs";
          }else{
            SQL = "select TwitterFollowerIDs.TwitterID from TwitterFollowerIDs\n"
                + "left outer join TwitterIDs\n"
                + "on TwitterFollowerIDs.TwitterID = TwitterIDs.TwitterID\n"
                + "where TwitterFollowerIDs.RemoveFollowerFlg = " + removeFollowerFlg + "\n";
            if(userPattern == UserPattern.NO_REMOVE_USER_AND_BANUSER) {
              SQL = SQL + "and TwitterIDs.BanUserFlg = " + banUserFlg + ";";
            }else if(userPattern == UserPattern.REMOVE_USER_AND_BANUSER){
              SQL = SQL + "or TwitterIDs.BanUserFlg = " + banUserFlg + ";";
            }
          }break;
        
        default:
          System.out.println("error:unknown TwitterIDPattern.");
          System.exit(1);
      }
      ResultSet result = statement.executeQuery(SQL);
      while(result.next()) {
        userList.add(result.getString(1));
      }
      result.close();
      statement.close();
      connection.close();
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
    return userList;
  }

  //TwitterIDs�ɓo�^����Ă���A�A�J�E���g��������Ă��郆�[�U�[���擾����
  public List<String> getTwitterIDBan() {
    List<String> TwitterIDList = new ArrayList<String>();
    String SQL = "";
    
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      SQL = "select TwitterIDs.TwitterID from TwitterIDs\n"
          + "where BanUserFlg = 1;";
      ResultSet result = statement.executeQuery(SQL);
      while(result.next()) {
        TwitterIDList.add(result.getString(1));
      }
      statement.close();
      connection.close();
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
    return TwitterIDList;
  }

  //TwitterIDs�ɓo�^����Ă���ARemoveFlg=1�̃��[�U�[���擾����
  public List<String> getTwitterIDRemove(int RemovePattern) {
    List<String> TwitterIDList = new ArrayList<String>();
    String SQL = "";
    
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      SQL = "select TwitterIDs.TwitterID from TwitterIDs\n"
          + "where BanUserFlg = 1;";
      ResultSet result = statement.executeQuery(SQL);
      while(result.next()) {
        TwitterIDList.add(result.getString(1));
      }
      statement.close();
      connection.close();
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
    return TwitterIDList;
  }
  
}















