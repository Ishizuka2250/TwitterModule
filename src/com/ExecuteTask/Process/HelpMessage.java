package com.ExecuteTask.Process;

public class HelpMessage implements Process{
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
    System.out.println("<Option>:[-c] フォロー・フォロワーの状態を取得する. *未登録ユーザのみユーザ情報を取得します.");
    System.out.println("         [-h] ヘルプを表示します. 他のオプションと同時に指定した場合ヘルプのみ表示されます.");
    System.out.println("");
  }
}
