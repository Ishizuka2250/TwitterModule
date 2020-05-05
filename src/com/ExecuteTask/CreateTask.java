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
import com.ExecuteTask.Process.UserCheck;

public class CreateTask {
  private Map<String,Process> ProcessMap;
  private Set<String> ExecuteOptionSet;
  private List<Process> ExecuteProcessList;
  
  public CreateTask(String args[]) {
    String userName;
    String optionCharacters;
    List<String> optionList = new ArrayList<String>();
    List<String> valueList = new ArrayList<String>();
    ExecuteProcessList = new ArrayList<Process>();
    ExecuteOptionSet = new HashSet<String>();
    
    if(args.length < 2) {
      ExecuteProcessList.add(new ArgsError("引数の指定に誤りがあります."));
    }else{
      userName = args[0];
      initProcess(userName);
      optionCharacters = setOptionCharacters("");
      
      for(int i = 1; i < args.length; i++) {
        if(args[i].matches("^-.+")){
          optionList.add(args[i]);
        }else{
          valueList.add(args[i]);
        }
      }
      
      if(optionList.size() > 0) {
        setProcess(optionList,valueList,optionCharacters);
      }else{
        ExecuteProcessList.add(new ArgsError("Optionを指定してください."));
      }
    }
  }

  private void setProcess(List<String> OptionList,List<String> ValueList,String optionCharacters) {
    String option;
    for(String argsOption : OptionList) {
      for(int j = 1; j < argsOption.length(); j++) {
        option = String.valueOf(argsOption.charAt(j));
        if (optionCharacters.contains(option) && !ExecuteOptionSet.contains(option) ) {
          ExecuteOptionSet.add(option);
          ExecuteProcessList.add(ProcessMap.get(option));
        }else if (option.equals("h")) {
          //help表示は例外対応
          ExecuteProcessList.clear();
          ExecuteProcessList.add(new HelpMessage(HelpMessage.HelpMessageType.FULL));
          return;
        }else if (!optionCharacters.contains(option)) {
          ExecuteProcessList.clear();
          ExecuteProcessList.add(new ArgsError("未定義のオプションです -- " + option));
          return;
        }
      }
    }
  }
  
  private String setOptionCharacters(String addOptionCharacter) {
    String options = "";
    for(String op : ProcessMap.keySet()) options += op;
    return options + addOptionCharacter;
  }
  
  public void execute() {
    for(Process p : ExecuteProcessList) p.execute();
  }

  private void initProcess(String UserName) {
    ProcessMap = new HashMap<String, Process>();
    ProcessMap.put("c", new UserCheck(UserName));
  }
  
}
