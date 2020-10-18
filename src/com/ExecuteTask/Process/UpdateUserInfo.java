package com.ExecuteTask.Process;
import com.TwitterModule.Twitter.TwitterIO.UpdatePattern;
import com.TwitterModule.Twitter.TwitterIO;

public class UpdateUserInfo implements IProcess{
  private UpdatePattern UpPattern;
  private String UserName;
  
  public UpdateUserInfo(String UserName, UpdatePattern Pattern){
    this.UserName = UserName;
    if ((Pattern != null) || (Pattern == UpdatePattern.UPDATE_NO_OPTION)) {
      this.UpPattern = Pattern;
    }else{
      this.UpPattern = UpdatePattern.UPDATE_NO_REMOVE_USER_AND_BANUSER; 
    }
  }
  
  @Override
  public void execute() {
    try {
      TwitterIO twitter = new TwitterIO(UserName);
      twitter.selectUserInfoUpdate(UpPattern);
      //System.out.println("UpdatePattern:" + UpPattern);
      //System.out.println("execute -> selectUserInfoUpdate()");
    }catch (Exception e) {
      System.out.println(e.toString());
    }
  }
  
}
