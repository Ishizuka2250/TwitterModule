package com.TwitterModule;

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

public class SqliteModule {
  private String SqliteDirPath;
  private String SqlitePath;
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  SqliteModule(String UserName) throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    SqliteDirPath = "D:/twitterApp";
    //SqliteDirPath = "/home/ishizuka/Temp/TwitterModule"; 
    SqlitePath = SqliteDirPath +  "/" + UserName + ".sqlite";
    //SqlitePath = SqliteDirPath +  "/" + UserName + ".sqlite";
    InitSqlite initSqlite = new InitSqlite(UserName,SqliteDirPath,SqlitePath);
    initSqlite.tableCheck(SqlitePath);
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
  public void updateTwitterID(Map<String,String> IDMap, int insertPattern) {
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
        if(insertPattern == 0) {
          SQL = "select Count(*) from TwitterIDs\n"
              + "where TwitterID = '" + id + "'";
        }else if(insertPattern == 1) {
          SQL = "select Count(*) from TwitterFollowIDs\n"
              + "where TwitterID = '" + id + "'";
        }else if(insertPattern == 2) {
          SQL = "select Count(*) from TwitterFollowerIDs\n"
              + "where TwitterID = '" + id + "'";
        }else {
          System.out.println("Unknown insertPattern -- " + insertPattern + "\nabort.");
          statement.close();
          connection.close();
          System.exit(1);
        }
        ResultSet result = statement.executeQuery(SQL);
        recordCount = result.getInt(1);
        
        //TwitterIDs/TwitterFollowIDs/TwitterFollowerIDs : ���R�[�h�V�K�ǉ���(recordCount = 0)
        if(recordCount == 0) {
          executeType = "insert";
          if(insertPattern == 0) {
            //id�̏�Ԃ�"0"�̏ꍇ:RemoveFlg = 0, id�̏�Ԃ�"1"�̏ꍇ:RemoveFlg = 1
            if(IDMap.get(id).equals("0")) {
              statement.execute("insert into TwitterIDs values('" + id + "', 0, 0, '" + sdf.format(cal.getTime()) + "', '000000000000', '0', '0', '0')");
            }else{
              statement.execute("insert into TwitterIDs values('" + id + "', 0, 0, '" + sdf.format(cal.getTime()) + "', '000000000000', '0', '1', '0')");
            }
          }else if(insertPattern == 1) {
            //id�̏�Ԃ�"0"�̏ꍇ:NotFollowFlg = 0, id�̏�Ԃ�"1"�̏ꍇ:NotFollowFlg = 1
            if(IDMap.get(id).equals("0")) {
              statement.execute("insert into TwitterFollowIDs values('" + id + "', '0', '0','" + sdf.format(cal.getTime()) + "');");
            }else{
              statement.execute("insert into TwitterFollowIDs values('" + id + "', '1', '0','" + sdf.format(cal.getTime()) + "');");
            }
          }else if(insertPattern == 2) {
            //id�̏�Ԃ�"0"�̏ꍇ:RemoveFollowFlg = 0, id�̏�Ԃ�"1"�̏ꍇ:RemoveFollowFlg = 1
            if(IDMap.get(id).equals("0")) {
              statement.execute("insert into TwitterFollowerIDs values('" + id + "', '0','" + sdf.format(cal.getTime()) + "');");
            }else{
              statement.execute("insert into TwitterFollowerIDs values('" + id + "', '1','" + sdf.format(cal.getTime()) + "');");
            }
          }
        }else{
          //TwitterIDs/TwitterFollowIDs/TwitterFollowerIDs : �������R�[�h�̍X�V (recordCount = 1)
          executeType = "update";
          //TwitterIDs�e�[�u���̍X�V
          if(insertPattern == 0) {
            if(IDMap.get(id).equals("0")) {
              statement.execute("update TwitterIDs set RemoveFlg = 0 where TwitterID = '" + id + "';");
            }else {
              statement.execute("update TwitterIDs set RemoveFlg = 1 where TwitterID = '" + id + "';");
            }
            statement.execute("update TwitterIDs set UpdateTime = '" + sdf.format(cal.getTime()) + "' where TwitterID = '" + id + "';");
          }
          //TwitterFollow�e�[�u���̍X�V
          if(insertPattern == 1) {
            if(IDMap.get(id).equals("0")) {
              statement.execute("update TwitterFollowIDs set NotFollowFlg = 0 where TwitterID = '" + id + "';");
            }else {
              statement.execute("update TwitterFollowIDs set NotFollowFlg = 1 where TwitterID = '" + id + "';");
            }
            statement.execute("update TwitterFollowIDs set UpdateTime = '" + sdf.format(cal.getTime()) + "' where TwitterID = '" + id + "';");
          }
          //TwitterFollower�e�[�u���̍X�V
          if(insertPattern == 2) {
            if(IDMap.get(id).equals("0")) {
              statement.execute("update TwitterFollowerIDs set RemoveFollowerFlg = 0 where TwitterID = '" + id + "';");
            }else {
              statement.execute("update TwitterFollowerIDs set RemoveFollowerFlg = 1 where TwitterID = '" + id + "';");
            }
            statement.execute("update TwitterFollowerIDs set UpdateTime = '" + sdf.format(cal.getTime()) + "' where TwitterID = '" + id + "';");
          }
          
        }
      }
      statement.execute("commit;");
      statement.close();
      connection.close();
      if(insertPattern == 0) System.out.println("squite.TwitterIDs " + executeType + " -- ok.");
      else if(insertPattern == 1) System.out.println("sqlite.TwitterFollowerIDs " + executeType + " -- ok.");
      else if(insertPattern == 2) System.out.println("sqlite.TwitterFollowIDs " + executeType + " -- ok.");
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
  public List<String> getTwitterIDList(int UserListFlg, int RemoveUserFlg) {
    List<String> userList = new ArrayList<String>();
    String SQL="";
    int banUserFlg,removeFlg,notFollowFlg,removeFollowerFlg;
    
    if((RemoveUserFlg == 0) || (RemoveUserFlg == 1)) {
      banUserFlg = removeFlg = notFollowFlg = removeFollowerFlg = 0;
    }else{
      banUserFlg = removeFlg = notFollowFlg = removeFollowerFlg = 1;
    }
    
    try{
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      if(UserListFlg == 0) {
        if(RemoveUserFlg == 0) {
          SQL = "select TwitterIDs.TwitterID from TwitterIDs";
        }else{
          SQL = "select TwitterIDs.TwitterID from TwitterIDs\n"
              + "where TwitterIDs.RemoveFlg = " + removeFlg + "\n";
          if(RemoveUserFlg == 1) {
            SQL = SQL + "and TwitterIDs.BanUserFlg = " + banUserFlg + ";";
          }else if(RemoveUserFlg == 2){
            SQL = SQL + "or TwitterIDs.BanUserFlg = " + banUserFlg + ";";
          }
        }
      }else if(UserListFlg == 1){
        if(RemoveUserFlg == 0) {
          SQL = "select TwitterFollowIDs.TwitterID from TwitterFollowIDs";
        }else{
          SQL = "select TwitterFollowIDs.TwitterID from TwitterFollowIDs\n"
              + "left outer join TwitterIDs\n"
              + "on TwitterFollowIDs.TwitterID = TwitterIDs.TwitterID\n"
              + "where TwitterFollowIDs.NotFollowFlg = " + notFollowFlg + "\n";
          if(RemoveUserFlg == 1) {
            SQL = SQL + "and TwitterIDs.BanUserFlg = " + banUserFlg + ";";
          }else if(RemoveUserFlg == 2){
            SQL = SQL + "or TwitterIDs.BanUserFlg = " + banUserFlg + ";";
          }
        }
      }else if(UserListFlg == 2) {
        if(RemoveUserFlg == 0) {
          SQL = "select TwitterFollowerIDs.TwitterID from TwitterFollowerIDs";
        }else{
          SQL = "select TwitterFollowerIDs.TwitterID from TwitterFollowerIDs\n"
              + "left outer join TwitterIDs\n"
              + "on TwitterFollowerIDs.TwitterID = TwitterIDs.TwitterID\n"
              + "where TwitterFollowerIDs.RemoveFollowerFlg = " + removeFollowerFlg + "\n";
          if(RemoveUserFlg == 1) {
            SQL = SQL + "and TwitterIDs.BanUserFlg = " + banUserFlg + ";";
          }else if(RemoveUserFlg == 2){
            SQL = SQL + "or TwitterIDs.BanUserFlg = " + banUserFlg + ";";
          }
        }
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















