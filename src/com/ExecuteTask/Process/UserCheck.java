package com.ExecuteTask.Process;

import com.TwitterModule.Twitter.TwitterIO;

public class UserCheck implements IProcess {
  private String UserName;
  public UserCheck(String userName){
    this.UserName = userName;
  }
  
  @Override
  public void execute() {
    try{
      //long start, end;
      //start = System.currentTimeMillis();
      TwitterIO twitter = new TwitterIO(UserName);
      
      //新規追加ユーザーのチェック
      twitter.twitterAddUserIDCheck();
      //フォロー・リフォローのチェック
      twitter.twitterUpdateUserIDCheck();
      
      //end = System.currentTimeMillis();
      //System.out.println("Info:更新が完了しました(" + (end - start) + "ms" + ").");
      //System.out.println("execute -> twitterAddUserIDCheck()");
    }catch (Exception e) {
      System.out.println(e.toString());
    }
  }
}
