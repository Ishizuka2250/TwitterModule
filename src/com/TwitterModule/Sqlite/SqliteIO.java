package com.TwitterModule.Sqlite;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Date;
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
  private String SqlitePath;
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  /** �f�[�^�i�[��̃e�[�u���� */
  public enum TableName {
    TWITTER_IDS,
    TWITTER_FOLLOW_IDS,
    TWITTER_FOLLOWER_IDS
  }
  
  /** �t�H���[�E�t�H�����[�̃p�^�[���w�� */
  public enum TwitterIDPattern {
    /** �SUserID(Follow_ID + Follower_ID)���w�� */
    ALL_ID,
    /** �t�H���[�������[�U�̂ݎw�� */
    FOLLOW_ID,
    /** �t�H���[����Ă��郆�[�U�̂ݎw�� */
    FOLLOWER_ID
  }
  
  /** �t�H���[�L���E�������[�U�̎w�� */
  public enum UserPattern {
    /** ���ׂẴ��[�U(�����w�薳��) */
    ALL,
    /** �Ўv���E���݃t�H���[���[�U��Ώ� */
    NO_REMOVE_USER_AND_BANUSER,
    /** �≏��ԁE��������Ă��郆�[�U��Ώ� */
    REMOVE_USER_AND_BANUSER
  }
  
  /** �R���X�g���N�^
   * @param UserName ���[�U��(UserScreenName)
   */
  public SqliteIO(String UserName) throws ClassNotFoundException {
    String cd = new File(".").getAbsoluteFile().getParent();
    Class.forName("org.sqlite.JDBC");
    //SqlitePath = SqliteDirPath +  "\\" + UserName + ".sqlite";
    SqlitePath = cd + "\\" + UserName + ".sqlite";
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
  /**
   * ���R�[�h�X�V����.<br>
   * �w�肳�ꂽ�e�[�u���̃t�H���[��Ԃ��X�V����. 
   * @param IDMap �X�V�Ώۂ�ID, �t�H���[��Ԃ�Map
   * @param updateTableName �X�V�Ώۃe�[�u����
   */
  public void updateTwitterID(Map<String, FollowState> IDMap, TableName updateTableName, String UpdateTime) {
    String SQL = "";
    String executeType = "";
    long recordCount;
    
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
                statement.execute("insert into TwitterIDs values('" + id + "', 0, 0, '" + UpdateTime + "', '000000000000', '0', '0', '0')");
              }else if(IDMap.get(id) == FollowState.REMOVE){
                statement.execute("insert into TwitterIDs values('" + id + "', 0, 0, '" + UpdateTime + "', '000000000000', '0', '1', '0')");
              }break;
            
            case TWITTER_FOLLOW_IDS:
              //id�̏�Ԃ�"0"�̏ꍇ:NotFollowFlg = 0, id�̏�Ԃ�"1"�̏ꍇ:NotFollowFlg = 1
              if(IDMap.get(id) == FollowState.FOLLOW) {
                statement.execute("insert into TwitterFollowIDs values('" + id + "', '0', '0','" + UpdateTime + "');");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOW){
                statement.execute("insert into TwitterFollowIDs values('" + id + "', '1', '0','" + UpdateTime + "');");
              }break;
              
            case TWITTER_FOLLOWER_IDS:
              //id�̏�Ԃ�"0"�̏ꍇ:RemoveFollowFlg = 0, id�̏�Ԃ�"1"�̏ꍇ:RemoveFollowFlg = 1
              if(IDMap.get(id) == FollowState.FOLLOWER) {
                statement.execute("insert into TwitterFollowerIDs values('" + id + "', '0','" + UpdateTime + "');");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOWER){
                statement.execute("insert into TwitterFollowerIDs values('" + id + "', '1','" + UpdateTime + "');");
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
              statement.execute("update TwitterIDs set UpdateTime = '" + UpdateTime + "' where TwitterID = '" + id + "';");
              break;
              
            //TwitterFollowIDs�X�V �� TwitterIDs �� UpdateTime �������ɍX�V
            case TWITTER_FOLLOW_IDS:
              if(IDMap.get(id) == FollowState.FOLLOW) {
                statement.execute("update TwitterFollowIDs set NotFollowFlg = 0 where TwitterID = '" + id + "';");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOW) {
                statement.execute("update TwitterFollowIDs set NotFollowFlg = 1 where TwitterID = '" + id + "';");
              }
              statement.execute("update TwitterFollowIDs set UpdateTime = '" + UpdateTime + "' where TwitterID = '" + id + "';");
              statement.execute("update TwitterIDs set UpdateTime = '" + UpdateTime + "' where TwitterID = '" + id + "';");
              break;
              
            //TwitterFollowerIDs�X�V �� TwitterIDs �� UpdateTime �������ɍX�V
            case TWITTER_FOLLOWER_IDS:
              if(IDMap.get(id) == FollowState.FOLLOWER) {
                statement.execute("update TwitterFollowerIDs set RemoveFollowerFlg = 0 where TwitterID = '" + id + "';");
              }else if(IDMap.get(id) == FollowState.NOT_FOLLOWER) {
                statement.execute("update TwitterFollowerIDs set RemoveFollowerFlg = 1 where TwitterID = '" + id + "';");
              }
              statement.execute("update TwitterFollowerIDs set UpdateTime = '" + UpdateTime + "' where TwitterID = '" + id + "';");
              statement.execute("update TwitterIDs set UpdateTime = '" + UpdateTime + "' where TwitterID = '" + id + "';");
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
  
  /**
   * ���[�U�����X�V���邽�߂̃t���O��On�ɂ���.
   * @param IDList ���[�U���X�V�Ώ�ID
   */
  public void updateFlgsOn(List<String> IDList) {
    String SQL = "";
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath,getProperties());
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      statement.execute("begin transaction");
      for(String id : IDList) {
        SQL = "update TwitterIDs set UpdateFlg = '1'\n"
            + "where TwitterID = '" + id + "'";
        statement.execute(SQL);
      }
      statement.execute("commit");
      statement.close();
      connection.close();
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
  }
  
  /**
   * ���[�U���X�V�Ώۂ�ID���擾����.
   * @return �X�V�Ώ�ID�̃��X�g
   */
  public List<String> getUpdateUserIDList() {
    List<String> TwitterIDList = new ArrayList<String>();
    String SQL = "";
    try{
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath,getProperties());
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      SQL = "select TwitterID from TwitterIDs\n"
          + "where UpdateFlg = '1'";
      ResultSet result = statement.executeQuery(SQL);
      while(result.next()) {
        TwitterIDList.add(result.getString(1));
      }
      statement.close();
      connection.close();
    }catch (SQLException e) {
      outputSQLStackTrace(e,SQL);
    }
    return TwitterIDList;
  }
  
  /**
   * ���[�U���̍X�V.<br>
   * �Â����R�[�h�����݂���ꍇ�͍폜���ĐV�������R�[�h��ǉ�����.
   * @param UserIDMap 
   * @return
   */
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
      for(String id : UserIDMap.keySet()) {
        SQL = "select count(*) from TwitterUserInfo\n"
            + "where TwitterID = '" + id + "'";
        result = statement.executeQuery(SQL);
        result.next();
        if(result.getInt(1) == 0) {
          //TwitterUserInfo�V�K�o�^��
          //�A�JBAN����Ă���UserID���ǂ����̔���[UserIDMap.get(id) = "1" �� �A�JBan] 
          if(UserIDMap.get(id).equals("1") == false) {//���[�U�[��񐳏�擾��(Not�A�JBAN)
            SQL = "insert into TwitterUserInfo values(" + UserIDMap.get(id) + ");";
            statement.execute(SQL);
            SQL = "update TwitterIDs set UpdateUserInfoTimes = 1,UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0', BanUserFlg = '0'\n"
                + "where TwitterID = '" + id + "'";
          }else{//�A�JBan
            //TwitterIDs���X�V�񐔂��擾
            SQL = "select * from TwitterIDs\n"
                + "where TwitterID = '" + id + "'";
            banCountResult = statement.executeQuery(SQL);
            banCountResult.next();
            updateUserInfoTimes = banCountResult.getInt(2);
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0', BanUserFlg = '1'\n"
                + "where TwitterID = '" + id + "'";
          }
          statement.execute(SQL);
        }else{//���[�U�[���o�^�ς� (�������R�[�h�̕����폜 �� �X�V���R�[�h�}��)
          //�ߋ��f�[�^�̕����폜
          SQL = "delete from TwitterUserInfo\n"
              + "where TwitterID = '" + id + "'";
          statement.execute(SQL);
          //TwitterIDs���X�V�񐔂��擾
          SQL = "select * from TwitterIDs\n"
              + "where TwitterID = '" + id + "'";
          result = statement.executeQuery(SQL);
          result.next();
          updateUserInfoTimes = result.getInt(2);
          //�A�JBAN����Ă��郆�[�U���ǂ�������. [UserIDMap.get(id) = "1" �� �A�JBan����Ă��郆�[�U]
          if(UserIDMap.get(id).equals("1") == false) {//���[�U�[��񐳏�擾��
            //User���X�V
            SQL = "insert into TwitterUserInfo values(" + UserIDMap.get(id) + ");";
            statement.execute(SQL);
            
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",  UpdateFlg = '0'\n" 
                + "where TwitterID = '" + id + "'";
          }else{//�A�JBan
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",  UpdateFlg = '0', BanUserFlg = '1'\n"
                + "where TwitterID = '" + id + "'";
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















