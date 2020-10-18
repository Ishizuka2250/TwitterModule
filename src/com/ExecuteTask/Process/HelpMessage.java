package com.ExecuteTask.Process;

public class HelpMessage implements IProcess{
  private HelpMessageType type;
  public enum HelpMessageType {
    SHORT,FULL;
  }
  
  public HelpMessage() {
    this.type = HelpMessageType.SHORT;
  }

  public HelpMessage(HelpMessageType type) {
    this.type = type;
  }
  
  @Override
  public void execute() {
    if (type == HelpMessageType.FULL) {
      System.out.println("アプリの概要:");
      System.out.println("  TwitterのAPIを使用してフォロー・フォロワーの状態をSqliteへ出力するバッチアプリケーションです.");
    }
    System.out.println("");
    System.out.println("使い方:");
    System.out.println("TwitterModule.jar <UserName> <Option>");
    System.out.println("");
    System.out.println("<UserName>:TwitterのScreenName (@から始まるID)");
    System.out.println("<Option>:[-c]  フォロー・フォロワーの状態を取得する. *未登録ユーザのみユーザ情報を取得します.");
    System.out.println("         [-h]  ヘルプを表示する. *他のオプションと同時に指定した場合ヘルプのみ表示されます.");
    System.out.println("         [-u] DBに登録されているユーザ情報を更新する(-a,-n,-b オプションと組み合わせて使用して下さい).");
    System.out.println("         [-a] DBに登録されている全ユーザ情報を更新する.");
    System.out.println("         [-n] DBに登録されている中で片思いもしくは相互フォローの関係にあるユーザ情報を更新する.");
    System.out.println("         [-b] DBに登録されている中で凍結垢・絶縁状態(片思い・相互フォローのどちらでもない)のユーザ情報を更新する. ");
    System.out.println("");
  }
}
