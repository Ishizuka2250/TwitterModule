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
  private String SqliteDirPath;
  private String SqlitePath;
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);
  
  SqliteResource(String UserName) throws ClassNotFoundException {
    SqliteDirPath = "D:/twitterApp";
    SqlitePath = SqliteDirPath +  "/" + UserName + ".sqlite";
    InitSqlite(UserName);
  }
  
  private void InitSqlite(String UserName) throws ClassNotFoundException  {
    Class.forName("org.sqlite.JDBC");
    File existSqlite = new File(SqlitePath);
    File existSqliteDir = new File(SqliteDirPath);
    if(existSqlite.exists() == false) {
      if(existSqliteDir.exists() == false) existSqliteDir.mkdirs();
      try {
        FileWriter emptySqlite = new FileWriter(existSqlite,false);
        emptySqlite.close();
        System.out.println("D:/twitterApp/に" + UserName + ".Sqlite を作成しました。");
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
  public void insertTwitterID(List<String> IDList, int insertPattern) {
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
              + "where TwitterID = '" + id + "'";
        }else if(insertPattern == 1) {
          SQL = "select Count(*) from TwitterFollowerIDs\n"
              + "where TwitterID = '" + id + "'";
        }else if(insertPattern == 2) {
          SQL = "select Count(*) from TwitterFollowIDs\n"
              + "where TwitterID = '" + id + "'";
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
          //アカBANされているUserIDかどうかの判定[UserIDMap.get(temp) = "BAN" → アカBan] 
          if(UserIDMap.get(temp).equals("BAN") == false) {//ユーザー情報正常取得時(NotアカBAN)
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
          //過去データの物理削除
          SQL = "delete from TwitterUserInfo\n"
              + "where TwitterID = '" + temp + "'";
          statement.execute(SQL);
          //TwitterIDsより更新回数を取得
          SQL = "select * from TwitterIDs\n"
              + "where TwitterID = '" + temp + "'";
          result = statement.executeQuery(SQL);
          result.next();
          updateUserInfoTimes = result.getInt(2);
          //アカBANされているUserIDかどうかの判定 [UserIDMap.get(temp) = "BAN" → アカBan]
          if(UserIDMap.get(temp).equals("BAN") == false) {//ユーザー情報正常取得時
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

  //UserListFlg = 0 twitterIDs
  //UserListFlg = 1 follower
  //UserListFlg = 2 follow
  //RemoveUser = true
  //  TwitterIDs:TwitterIDsに登録されているIDをすべて取得する。
  //  TwitterFollowerIDs:TwitterFollowerIDsに登録されているIDをすべて取得する。
  //  TwitterFollowIDs:TwitterFollowIDsに登録されているIDをすべて取得する。
  //RemoveUser = False
  //  TwitterIDs:フォローもフォロバもされていないユーザー かつ 凍結されているユーザーは除外する。
  //  TwitterFollowerIDs:フォロバされていないユーザー かつ 凍結されているユーザー は除外する。
  //  TwitterFollowIDs:リフォローしているユーザー かつ 凍結されているユーザー は除外する。
  public List<String> getTwitterIDList(int UserListFlg, Boolean RemoveUser) {
    List<String> userList = new ArrayList<String>();
    String SQL="";
    try{
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      if(UserListFlg == 0) {
        if(RemoveUser) {
          SQL = "select TwitterIDs.TwitterID from TwitterIDs";
        }else{
          SQL = "select TwitterIDs.TwitterID from TwitterIDs\n"
              + "where TwitterIDs.BanUserFlg = 0\n"
              + "and TwitterIDs.RemoveFlg = 0;";
        }
      }else if(UserListFlg == 1){
        if(RemoveUser) {
          SQL = "select TwitterFollowerIDs.TwitterID from TwitterFollowerIDs";
        }else{
          SQL = "select TwitterFollowerIDs.TwitterID from TwitterFollowerIDs\n"
              + "left outer join TwitterIDs\n"
              + "on TwitterFollowerIDs.TwitterID = TwitterIDs.TwitterID\n"
              + "where TwitterFollowerIDs.RemoveFollowFlg = 0\n"
              + "and TwitterIDs.BanUserFlg = 0;";
        }
      }else if(UserListFlg == 2) {
        if(RemoveUser) {
          SQL = "select TwitterFollowIDs.TwitterID from TwitterFollowIDs";
        }else{
          SQL = "select TwitterFollowIDs.TwitterID from TwitterFollowIDs\n"
              + "left outer join TwitterIDs\n"
              + "on TwitterFollowIDs.TwitterID = TwitterIDs.TwitterID\n"
              + "where TwitterFollowIDs.RemoveFollowFlg = 0\n"
              + "and TwitterIDs.BanUserFlg = 0;";
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

  //TwitterIDsに登録されており、アカウント凍結されているユーザーを取得する
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

  //TwitterIDsに登録されており、RemoveFlg=1のユーザーを取得する
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
  
  //UserListFlg = 0 TwitterIDs
  //UserListFlg = 1 FollowerID
  //UserListFlg = 2 followID
  //RemoveFlg = true Remove系のフラグを0にする
  //RemoveFlg = false Remove系のフラグを1にする
  public void updateRemoveFlgs(List<String> UserIDList, int UserListFlg, boolean RemoveFlg) {
    Calendar cal = Calendar.getInstance();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
    String SQL,userIDs;
    int removeValue = 0;
    SQL = userIDs = "";
    if(!RemoveFlg) removeValue = 1; 
    try {
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + SqlitePath);
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      
      //UserIDList.forEach(s -> {if(userIDs.equals("")) {userIDs = "'" + s;} else {userIDs = "','" + userIDs + "'";}});
      for(String temp : UserIDList) {
        if(userIDs.equals("")) userIDs = "'" + temp + "'";
        else userIDs = userIDs + ",'" + temp + "'";
      }
      if(UserListFlg == 0) {
        SQL = "update TwitterIDs Set RemoveFlg = " + removeValue + "\n"
            + "where TwitterID in(" + userIDs + ")";
      }else if(UserListFlg == 1) {
        SQL = "update TwitterFollowerIDs Set RemoveFollowFlg = " + removeValue + "\n"
            + "where TwitterID in(" + userIDs + ")";
      }else if(UserListFlg == 2) {
        SQL = "update TwitterFollowIDs Set RemoveFollowFlg = " + removeValue + "\n"
            + "where TwitterID in(" + userIDs + ")";
      }
      statement.execute("begin transaction;");
      statement.execute(SQL);
      SQL = "update TwitterIDs set UpdateTime = " + sdf.format(cal.getTime()) + "\n"
          + "where TwitterID in(" + userIDs + ")";
      statement.execute(SQL);
      statement.execute("commit;");
      
      statement.close();
      connection.close();
    }catch (SQLException e){
      outputSQLStackTrace(e,SQL);
    }
    
  }
  
}















