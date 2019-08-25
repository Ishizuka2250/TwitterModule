package com.TwitterModule.Twitter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import twitter4j.User;

public class TwitterUserInfo {
  public String ID;
  public String UserName;
  public String UserScreenName;
  public String UserDescription;
  public long UserTweetCount;
  public long UserFollowCount;
  public long UserFollowerCount;
  public long UserFavoritesCount;
  public String UserLocation;
  public String UserCreatedAt;
  public String UserBackGroundColor;
  
  public TwitterUserInfo(User user) {
    UserName = sanitizing(user.getName());
    UserScreenName = sanitizing(user.getScreenName());
    UserDescription = sanitizing(user.getDescription());
    UserTweetCount = user.getStatusesCount();
    UserFollowCount = user.getFriendsCount();
    UserFollowerCount = user.getFollowersCount();
    UserFavoritesCount = user.getFavouritesCount();
    UserLocation = sanitizing(user.getLocation());
    UserCreatedAt = String.valueOf(user.getCreatedAt());
    UserBackGroundColor = user.getProfileBackgroundColor();
  }
  
  private String sanitizing(String str) {
    return replaceString(str, "'", "''");
  }
  
  private String replaceString(String str, String replace, String replacement) {
    String reg = replace;
    Pattern pat = Pattern.compile(reg);
    Matcher mat = pat.matcher(str);
    return mat.replaceAll(replacement);
  }
  
  @Override
  public String toString() {
    return "'" + ID + "','" + UserName + "','" + UserScreenName + "','"
      + UserDescription + "','" + UserTweetCount + "','" + UserFollowCount
      + "','" + UserFollowerCount + "','" + UserFavoritesCount + "','"
      + UserLocation + "','" + UserCreatedAt + "','" + UserBackGroundColor + "'";
  }
  
}
