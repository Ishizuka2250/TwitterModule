

import java.io.IOException;

import twitter4j.TwitterException;

import com.TwitterModule.Twitter.TwitterIO;

public class Main {
  public static void main(String args[]) throws TwitterException, IOException,ClassNotFoundException {
    long start, end, TwitterIDsTime;
    String userName="";
    
    if(args.length == 0) {
      System.out.println("Error:���[�U�[������͂��ĉ�����.");
      System.exit(1);
    }else{
      userName = args[0];
    }
      
    TwitterIO twitter = new TwitterIO(userName);
    start = System.currentTimeMillis();
    
    //�V�K�ǉ����[�U�[�̃`�F�b�N
    twitter.twitterAddUserIDCheck();
    //�t�H���[�E���t�H���[�̃`�F�b�N
    twitter.twitterUpdateUserIDCheck();
    end = System.currentTimeMillis();

    TwitterIDsTime = end - start;
    System.out.println("Info:�X�V���������܂���(" + TwitterIDsTime + "ms" + ").");
  }

}
