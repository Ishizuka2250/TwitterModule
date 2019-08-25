

import java.io.IOException;
import twitter4j.TwitterException;
import com.TwitterModule.TwitterModule;

public class Main {
  public static void main(String args[]) throws TwitterException, IOException,ClassNotFoundException {
    long start, end, TwitterIDsTime;
    String userName="";
    
    if(args.length == 0) {
      System.out.println("Error:ユーザー名を入力して下さい.");
      System.exit(1);
    }else{
      userName = args[0];
    }
      
    TwitterModule twitter = new TwitterModule(userName);
    start = System.currentTimeMillis();
    
    //新規追加ユーザーのチェック
    twitter.twitterAddUserIDCheck();
    //フォロー・リフォローのチェック
    twitter.twitterUpdateUserIDCheck();
    end = System.currentTimeMillis();

    TwitterIDsTime = end - start;
    System.out.println("Info:更新が完了しました(" + TwitterIDsTime + "ms" + ").");
  }

}
