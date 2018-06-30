package com.TwitterModule;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

public class SqliteResource {
  private String SqlitePath = "D:/twitterApp/Twitter.sqlite";
  private String SqliteDirPath = "D:/twitterApp";
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  SqliteResource() throws ClassNotFoundException {
    InitSqlite();
  }
  
  private void InitSqlite() throws ClassNotFoundException  {
    Class.forName("org.sqlite.JDBC");
    File existSqlite = new File(SqlitePath);
    File existSqliteDir = new File(SqliteDirPath);
    if(existSqlite.exists() == false) {
      if(existSqliteDir.exists() == false) existSqliteDir.mkdirs();
      try {
        FileWriter emptySqlite = new FileWriter(existSqlite,false);
        emptySqlite.close();
        System.out.println("D:/twitterApp/にTwitter.Sqlite を作成しました。");
      } catch (IOException e) {
        e.printStackTrace(pw);
        pw.flush();
        return;
      }
    }
    TableCheck();
  }
  
  private boolean TableCheck() {
    boolean TwitterIDsExists = false;
    boolean TwitterFollowerIDsExists = false;
    boolean TwitterFollowIDsExists = false;
    boolean TwitterUserInfoExists = false;
    boolean TwitterUserInfoURLsExists = false;
    boolean UnReturnFollowIDsExists = false;
    boolean UnFollowIDsExists = false;
    boolean BufferTwitterIDsExists = false;
    boolean TableCheckStatus = true;
    String SQL = "";
    
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      
      ResultSet result = statement.executeQuery("select * from sqlite_master");
      while(result.next()) {
        if(result.getString(2).equals("TwitterIDs")) TwitterIDsExists = true;
        if(result.getString(2).equals("TwitterFollowerIDs")) TwitterFollowerIDsExists = true;
        if(result.getString(2).equals("TwitterFollowIDs")) TwitterFollowIDsExists = true;
        if(result.getString(2).equals("TwitterUserInfo")) TwitterUserInfoExists = true;
        if(result.getString(2).equals("TwitterUserInfoURLs")) TwitterUserInfoURLsExists = true;
        if(result.getString(2).equals("UnReturnFollowIDs")) UnReturnFollowIDsExists = true;
        if(result.getString(2).equals("UnFollowIDs")) UnFollowIDsExists = true;
        //if(result.getString(2).equals("BufferTwitterIDs")) BufferTwitterIDsExists = true;
      }
      
      if(!TwitterIDsExists) {
        SQL = "create table TwitterIDs("
            + "TwitterID text primary key,"
            + "UpdateUserInfoTimes integer,"
            + "UpdateUserInfoURLTimes integer,"
            + "CreateTime text,"
            + "UpdateTime text,"
            + "BanUserFlg boolean,"
            + "RemoveFlg boolean,"
            + "UpdateFlg boolean);";
        statement.execute(SQL);
      }
      
      if(!TwitterFollowerIDsExists) {
        SQL = "create table TwitterFollowerIDs("
            + "TwitterID text primary key,"
            + "RemoveFollowFlg boolean);";
        statement.execute(SQL);
      }
      if(!TwitterFollowIDsExists) {
        SQL = "create table TwitterFollowIDs("
            + "TwitterID text primary key,"
            + "RemoveFollowFlg boolean,"
            + "FavoriteFlg boolean);";
        statement.execute(SQL);
      }
      if(!TwitterUserInfoExists) {
        SQL = "create table TwitterUserInfo("
            + "TwitterID text primary key,"
            + "UserName text,"
            + "UserScreenName text,"
            + "UserDescripton text,"
            + "UserTweetCount text,"
            + "UserFollowCount text,"
            + "UserFollowerCount text,"
            + "UserFavouritesCount text,"
            + "UserLocation text,"
            + "UserCreatedAt text,"
            + "UserBackGroundColor text);";
        statement.execute(SQL);
      }
      if(!TwitterUserInfoURLsExists) {
        SQL = "create table TwitterUserInfoURLs("
            + "TwitterID text primary key,"
            + "UserURL text,"
            + "UserOriginalProfileImageURL text,"
            + "UserProfileBackGroundImageURL text);";
        statement.execute(SQL);
      }
      if(!UnReturnFollowIDsExists) {
        SQL = "create view UnReturnFollowIDs as\n"
            + "select TwitterID from TwitterFollowIDs\n"
            + "where not exists(\n"
            + "  select TwitterFollowerIDs.TwitterID\n"
            + "  from TwitterFollowerIDs\n"
            + "  where TwitterFollowIDs.TwitterID = TwitterFollowerIDs.TwitterID);";
        statement.execute(SQL);
      }
      if(!UnFollowIDsExists) {
        SQL = "create view UnFollowIDs as\n"
            + "select TwitterID from TwitterFollowerIDs\n"
            + "where not exists(\n"
            + "  select TwitterFollowIDs.TwitterID from TwitterFollowIDs\n"
            + "  where TwitterFollowIDs.TwitterID = TwitterFollowerIDs.TwitterID);";
        statement.execute(SQL);
      }
      statement.close();
      connection.close();
    }catch (SQLException e) {
      outputSQLStackTrace(e,SQL);
    }
    return TableCheckStatus;
  }
  
  //sqliteのpragma設定
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
  
  //insertPattern = 0 TwitterIDs
  //insertPattern = 1 FollowerID
  //insertPattern = 2 followID
  public void insertTwitterIDs(List<String> IDList, int insertPattern) {
    String SQL = "";
    long count;
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath, getProperties());
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      statement.execute("begin transaction;");
      for(String id : IDList) {
        if(insertPattern == 0) {
          SQL = "select Count(*) from TwitterIDs\n"
              + "where TwitterID = " + id;
        }else if(insertPattern == 1) {
          SQL = "select Count(*) from TwitterFollowerIDs\n"
              + "where TwitterID = " + id;
        }else if(insertPattern == 2) {
          SQL = "select Count(*) from TwitterFollowIDs\n"
              + "where TwitterID = " + id;
        }else {
          System.out.println("Unknown insertPattern -- " + insertPattern + "\nabort.");
          statement.close();
          connection.close();
          System.exit(1);
        }
        ResultSet result = statement.executeQuery(SQL);
        count = result.getInt(1);
        if(count == 0) {
          if(insertPattern == 0) {
            statement.execute("insert into TwitterIDs values('" + id + "', 0, 0, '" + sdf.format(cal.getTime()) + "', '000000000000', '0', '0', '0')");
          }else if(insertPattern == 1) {
            statement.execute("insert into TwitterFollowerIDs values('" + id + "', '0');");
          }else{
            statement.execute("insert into TwitterFollowIDs values('" + id + "', '0', '0');");
          }
        }
      }
      statement.execute("commit;");
      statement.close();
      connection.close();
      if(insertPattern == 0) System.out.println("TwitterIDs insert ok.");
      else if(insertPattern == 1) System.out.println("TwitterFollowerIDs insert ok.");
      else if(insertPattern == 2) System.out.println("TwitterFollowIDs insert ok.");
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
  
  public List<String> getTwitterIDAll() {
    List<String> TwitterIDList = new ArrayList<String>();
    String SQL = "";
    
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      SQL = "select TwitterID from TwitterFollowerIDs\n"
          + "union\n"
          + "select TwitterID from TwitterFollowIDs;";
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
          //TwitterUserInfo新規登録時
          //アカBANされているUserIDかどうかの判定 
          if(UserIDMap.get(temp).equals("1") == false) {//ユーザー情報正常取得時
            SQL = "insert into TwitterUserInfo values(" + UserIDMap.get(temp) + ");";
            statement.execute(SQL);
            SQL = "update TwitterIDs set UpdateUserInfoTimes = 1,UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0', BanUserFlg = '0'\n"
                + "where TwitterID = '" + temp + "'";
          }else{//アカBan
            //TwitterIDsより更新回数を取得
            SQL = "select * from TwitterIDs\n"
                + "where TwitterID = '" + temp + "'";
            banCountResult = statement.executeQuery(SQL);
            banCountResult.next();
            updateUserInfoTimes = banCountResult.getInt(2);
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0', BanUserFlg = '1'\n"
                + "where TwitterID = '" + temp + "'";
          }
          statement.execute(SQL);
        }else{//既にユーザー情報が登録済み
          //過去データの削除
          SQL = "delete from TwitterUserInfo\n"
              + "where TwitterID = '" + temp + "'";
          statement.execute(SQL);
          //TwitterIDsより更新回数を取得
          SQL = "select * from TwitterIDs\n"
              + "where TwitterID = '" + temp + "'";
          result = statement.executeQuery(SQL);
          result.next();
          updateUserInfoTimes = result.getInt(2);
          //アカBANされているUserIDかどうかの判定 [UserIDMap.get(temp) = "1" → アカBan]
          if(UserIDMap.get(temp).equals("1") == false) {//ユーザー情報正常取得時
            //User情報更新
            SQL = "insert into TwitterUserInfo values(" + UserIDMap.get(temp) + ");";
            statement.execute(SQL);
            
            SQL = "update TwitterIDs set UpdateUserInfoTimes = " + (updateUserInfoTimes + 1) + ",UpdateTime = " + sdf.format(cal.getTime()) + ",  UpdateFlg = '0'\n"
                + "where TwitterID = '" + temp + "'";
          }else{//アカBan
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

  //UserListFlg = 0 follower
  //UserListFlg = 1 follow
  public List<String> getTwitterIDList(int UserListFlg) {
    List<String> userList = new ArrayList<String>();
    String SQL="";
    try{
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      if(UserListFlg == 0){
        SQL = "select * from TwitterFollowerIDs\n"
            + "where GetUserInfo = 0";
      }else if(UserListFlg == 1) {
        SQL = "select * from TwitterFollowIDs\n"
            + "where GetUserInfo = 0";
      }
      ResultSet result = statement.executeQuery(SQL);
      while(result.next()) {
        userList.add(result.getString(1));
      }
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
    return userList;
  }

  
}















