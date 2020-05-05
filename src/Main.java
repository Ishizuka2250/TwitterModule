import com.ExecuteTask.CreateTask;

public class Main {
  public static void main(String args[]){
    //TwitterModule.jar <UserName> -c
    CreateTask task = new CreateTask(args);
    task.execute();
  }
}
