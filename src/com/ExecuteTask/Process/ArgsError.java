package com.ExecuteTask.Process;
import com.ExecuteTask.Process.HelpMessage;

public class ArgsError implements Process{
  private String ErrorMessage;
  private HelpMessage Help;
  public ArgsError(String ErrorMsg) {
    this.ErrorMessage = ErrorMsg;
    this.Help = new HelpMessage();
  }

  @Override
  public void execute(){
    System.out.println("Error:" + ErrorMessage);
    Help.execute();
  }
}
