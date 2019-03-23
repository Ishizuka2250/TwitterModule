package com.TwitterModule;

import java.io.*;
import twitter4j.*;

public class Main {
  public static void main(String args[]) throws TwitterException, IOException,ClassNotFoundException {
    long start, end, TwitterIDsTime;
    TwitterModule twitter = new TwitterModule();
    
    start = System.currentTimeMillis();
    //新規追加ユーザーのチェック
    twitter.twitterAddUserIDCheck();
    //フォロー・リフォローのチェック
    twitter.twitterUpdateUserIDCheck();
    end = System.currentTimeMillis();
    TwitterIDsTime = end - start;
    System.out.println(TwitterIDsTime + "ms");
    System.out.println("done!");
  }

}
