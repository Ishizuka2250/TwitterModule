package com.TwitterModule;

import java.io.*;
import twitter4j.*;

public class Main {
  public static void main(String args[]) throws TwitterException, IOException,ClassNotFoundException {
    long start, end, TwitterIDsTime;
    TwitterModule twitter = new TwitterModule();
    
    start = System.currentTimeMillis();
    //�V�K�ǉ����[�U�[�̃`�F�b�N
    twitter.twitterAddUserIDCheck();
    //�t�H���[�E���t�H���[�̃`�F�b�N
    twitter.twitterUpdateUserIDCheck();
    end = System.currentTimeMillis();
    TwitterIDsTime = end - start;
    System.out.println(TwitterIDsTime + "ms");
    System.out.println("done!");
  }

}
