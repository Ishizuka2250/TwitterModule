package com.TwitterModule;

import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class InitSqlite {
  private StringWriter StackTrace = new StringWriter();
  private PrintWriter pw = new PrintWriter(StackTrace);

  InitSqlite(String UserName, String SqliteDirPath, String SqlitePath) throws ClassNotFoundException  {
    File existSqlite = new File(SqlitePath);
    File existSqliteDir = new File(SqliteDirPath);
    if(existSqlite.exists() == false) {
      if(existSqliteDir.exists() == false) existSqliteDir.mkdirs();
      try {
        FileWriter emptySqlite = new FileWriter(existSqlite,false);
        emptySqlite.close();
        System.out.println("Info:[" + existSqlite.getParent() + "]に" + UserName + ".Sqlite を作成しました。");
      } catch (IOException e) {
        e.printStackTrace(pw);
        pw.flush();
        return;
      }
    }
  }

  public boolean tableCheck(String SqlitePath) {
    boolean TwitterIDsExists = false;
    boolean TwitterFollowerIDsExists = false;
    boolean TwitterFollowIDsExists = false;
    boolean TwitterUserInfoExists = false;
    boolean TwitterUserInfoURLsExists = false;
    boolean RemoveFollowerIDsViewExists = false;
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
        if(result.getString(2).equals("RemoveFollowerIDs")) RemoveFollowerIDsViewExists = true;
        if(result.getString(2).equals("UnFollowIDs")) UnFollowIDsExists = true;
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
            + "RemoveFollowerFlg boolean,"
            + "UpdateTime Text);";
        statement.execute(SQL);
      }
      if(!TwitterFollowIDsExists) {
        SQL = "create table TwitterFollowIDs("
            + "TwitterID text primary key,"
            + "NotFollowFlg boolean,"
            + "FavoriteFlg boolean,"
            + "UpdateTime Text);";
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
      if(!RemoveFollowerIDsViewExists) {
        SQL = "create view RemoveFollowerIDs as\n"
            + "select TwitterIDs.TwitterID,TwitterUserInfo.UserScreenName,TwitterUserInfo.UserName\n"
            + "from TwitterIDs\n"
            + "left outer join TwitterFollowIDs on TwitterIDs.TwitterID = TwitterFollowIDs.TwitterID\n"
            + "left outer join TwitterFollowerIDs on TwitterIDs.TwitterID = TwitterFollowerIDs.TwitterID\n"
            + "left outer join TwitterUserInfo on TwitterIDs.TwitterID = TwitterUserInfo.TwitterID\n"
            + "where TwitterFollowerIDs.RemoveFollowerFlg = 1\n"
            + "and TwitterIDs.RemoveFlg != 1\n"
            + "and TwitterFollowIDs.FavoriteFlg = 0";
        statement.execute(SQL);
      }
      if(!UnFollowIDsExists) {
        SQL = "create view UnFollowIDs as\n"
            + "select TwitterIDs.TwitterID, TwitterUserInfo.UserScreenName, TwitterUserInfo.UserName\n"
            + "from TwitterIDs\n"
            + "left outer join TwitterUserInfo on TwitterIDs.TwitterID = TwitterUserInfo.TwitterID\n"
            + "left outer join TwitterFollowIDs on TwitterIDs.TwitterID = TwitterFollowIDs.TwitterID\n"
            + "where TwitterFollowIDs.NotFollowFlg = 1";
        statement.execute(SQL);
      }
      statement.close();
      connection.close();
    }catch (SQLException e) {
      outputSQLStackTrace(e,SQL);
    }
    return TableCheckStatus;
  }

  private void outputSQLStackTrace(SQLException e,String SQL) {
    e.printStackTrace(pw);
    pw.flush();
    System.out.println("Error:クエリの実行に失敗しました.");
    System.out.println(StackTrace.toString());
    System.out.println("--Missing SQL------------------------");
    System.out.println(SQL);
    System.out.println("-------------------------------------");
    System.exit(1);
  }
  
}
