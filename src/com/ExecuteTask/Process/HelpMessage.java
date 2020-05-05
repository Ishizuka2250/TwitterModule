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
      System.out.println("�A�v���̊T�v:");
      System.out.println("  Twitter��API���g�p���ăt�H���[�E�t�H�����[�̏�Ԃ�Sqlite�֏o�͂���o�b�`�A�v���P�[�V�����ł�.");
    }
    System.out.println("");
    System.out.println("�g����:");
    System.out.println("TwitterModule.jar <UserName> <Option>");
    System.out.println("");
    System.out.println("<UserName>:Twitter��ScreenName (@����n�܂�ID)");
    System.out.println("<Option>:[-c] �t�H���[�E�t�H�����[�̏�Ԃ��擾����. *���o�^���[�U�̂݃��[�U�����擾���܂�.");
    System.out.println("         [-h] �w���v��\�����܂�. ���̃I�v�V�����Ɠ����Ɏw�肵���ꍇ�w���v�̂ݕ\������܂�.");
    System.out.println("");
  }
}
