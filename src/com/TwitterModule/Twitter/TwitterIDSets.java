package com.TwitterModule.Twitter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import com.TwitterModule.Sqlite.SqliteIO;
import com.TwitterModule.Sqlite.SqliteIO.TwitterIDPattern;
import com.TwitterModule.Sqlite.SqliteIO.UserPattern;

public class TwitterIDSets {
  private Twitter twitter;
  public Set<String> FollowIDSet = new HashSet<String>();
  public Set<String> FollowerIDSet = new HashSet<String>();
  public Set<String> AllIDSet = new HashSet<String>();
  public Set<String> RemoveFollowIDSet = new HashSet<String>();
  public Set<String> RemoveFollowerIDSet = new HashSet<String>();
  
  public TwitterIDSets(Twitter twitter) {
    this.twitter = twitter;
    try{
      getTwitterFollowIDList().forEach(it -> {
        FollowIDSet.add(it);
        AllIDSet.add(it);
      });
      getTwitterFollowerIDList().forEach(it -> {
        FollowerIDSet.add(it);
        AllIDSet.add(it);
      });
    }catch(TwitterException te) {
      System.out.println("Error:TwitterIDSets:Throw TwitterException.");
      System.out.println(te.toString());
      System.exit(1);
    }
  }

  public TwitterIDSets(SqliteIO sqlite) {
    sqlite.getTwitterIDList(TwitterIDPattern.ALL_ID, UserPattern.ALL).forEach(it -> {
      AllIDSet.add(it);
    });
    sqlite.getTwitterIDList(TwitterIDPattern.FOLLOW_ID, UserPattern.FOLLOW_AND_FOLLOWER).forEach(it -> {
      FollowIDSet.add(it);
    });
    sqlite.getTwitterIDList(TwitterIDPattern.FOLLOWER_ID, UserPattern.FOLLOW_AND_FOLLOWER).forEach(it -> {
      FollowerIDSet.add(it);
    });
    sqlite.getTwitterIDList(TwitterIDPattern.FOLLOW_ID, UserPattern.REMOVE_USER_AND_BANUSER).forEach(it -> {
      RemoveFollowIDSet.add(it);
    });
    sqlite.getTwitterIDList(TwitterIDPattern.FOLLOWER_ID, UserPattern.REMOVE_USER_AND_BANUSER).forEach(it -> {
      RemoveFollowerIDSet.add(it);
    });
  }
  
  private List<String> getTwitterFollowIDList() throws TwitterException {
    long cursol = -1L;
    //Twitter twitter = TwitterCore.getTwitterInstance();
    IDs ids;
    List<String> IDList = new ArrayList<String>();
    do {
      ids = twitter.getFriendsIDs(cursol);
      for (long temp : ids.getIDs()) {
        IDList.add(String.valueOf(temp));
      }
    } while (ids.hasNext());
    return IDList;
  }

  private List<String> getTwitterFollowerIDList() throws TwitterException {
    long cursol = -1L;
    //Twitter twitter = TwitterCore.getTwitterInstance();
    IDs ids;
    List<String> IDList = new ArrayList<String>();
    do {
      ids = twitter.getFollowersIDs(cursol);
      for (long temp : ids.getIDs()) {
        IDList.add(String.valueOf(temp));
      }
    } while (ids.hasNext());
    return IDList;
  }
  
}
