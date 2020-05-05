package com.ExecuteTask;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import com.ExecuteTask.Process.ArgsError;
import com.ExecuteTask.Process.HelpMessage;
import com.ExecuteTask.Process.Process;
import com.ExecuteTask.Process.UpdateUserInfo;
import com.ExecuteTask.Process.UserCheck;
import com.TwitterModule.Twitter.TwitterIO.UpdatePattern;

public class CreateTask {
  private Map<String,Process> ProcessMap;
  private Set<String> ExecuteOptionSet;
  private List<Process> ExecuteProcessList;
  private String OptionCharacters;
  private String SkipCharacters;
  
  public CreateTask(String args[]) {
    String userName;
    List<String> optionList = new ArrayList<String>();
    List<String> valueList = new ArrayList<String>();
    ExecuteProcessList = new ArrayList<Process>();
    ExecuteOptionSet = new HashSet<String>();
    
    if(args.length < 2) {
      outputErrorMessage("引数の指定に誤りがあります.");
    }else{
      userName = args[0];
      
      for(int i = 1; i < args.length; i++) {
        if(args[i].matches("^-.+")){
          optionList.add(args[i]);
        }else{
          valueList.add(args[i]);
        }
      }
      
      if(optionList.size() > 0) {
        initProcess(optionList,userName);
        initOptionCharacters("anb");
        setProcess(optionList,valueList);
      }else{
        outputErrorMessage("Optionを指定してください.");
      }
    }
  }

  private void setProcess(List<String> OptionList,List<String> ValueList) {
    String option;
    UpdatePattern updatePattern = getUpdatePattern(OptionList);
    for (String argsOption : OptionList) {
      for (int i = 1; i < argsOption.length(); i++) {
        option = String.valueOf(argsOption.charAt(i));
        if (SkipCharacters.contains(option)) {
          if (updatePattern == null) {
            outputErrorMessage("[-u] オプションと組み合わせて使用して下さい -- " + option);
            return;
          }continue;
        }
        if (OptionCharacters.contains(option) && !ExecuteOptionSet.contains(option) ) {
          ExecuteOptionSet.add(option);
          ExecuteProcessList.add(ProcessMap.get(option));
        }else if (option.equals("h")) {
          //help表示は例外対応
          ExecuteProcessList.clear();
          ExecuteProcessList.add(new HelpMessage(HelpMessage.HelpMessageType.FULL));
          return;
        }else if (!OptionCharacters.contains(option)) {
          outputErrorMessage("未定義のオプションです -- " + option);
          return;
        }
      }
    }
  }
  
  private void initOptionCharacters(String skip) {
    OptionCharacters = "";
    SkipCharacters = "";
    for(String op : ProcessMap.keySet()) OptionCharacters += op;
    SkipCharacters = skip;
  }
  
  private UpdatePattern getUpdatePattern(List<String> OptionList) {
    boolean update = false;
    for(String updateCheck : OptionList) {
      if(updateCheck.contains("u")) update = true;
    }
    if(update) {
      for(String argsOption : OptionList) {
        if(argsOption.contains("a")) return UpdatePattern.UPDATE_ALL;
        if(argsOption.contains("n")) return UpdatePattern.UPDATE_NO_REMOVE_USER_AND_BANUSER;
        if(argsOption.contains("b")) return UpdatePattern.UPDATE_BANUSER;
      }
      return UpdatePattern.UPDATE_NO_OPTION;
    }
    return null;
  }
  
  private void outputErrorMessage(String ErrorMessage) {
    ExecuteProcessList.clear();
    ExecuteProcessList.add(new ArgsError(ErrorMessage));
  }
  
  public void execute() {
    for(Process p : ExecuteProcessList) p.execute();
  }

  private void initProcess(List<String> OptionList,String UserName) {
    ProcessMap = new HashMap<String, Process>();
    ProcessMap.put("c", new UserCheck(UserName));
    ProcessMap.put("u", new UpdateUserInfo(UserName, getUpdatePattern(OptionList)));
  }
  
}
