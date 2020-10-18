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
      System.out.println("�A�v���̊T�v:");
      System.out.println("  Twitter��API���g�p���ăt�H���[�E�t�H�����[�̏�Ԃ�Sqlite�֏o�͂���o�b�`�A�v���P�[�V�����ł�.");
    }
    System.out.println("");
    System.out.println("�g����:");
    System.out.println("TwitterModule.jar <UserName> <Option>");
    System.out.println("");
    System.out.println("<UserName>:Twitter��ScreenName (@����n�܂�ID)");
    System.out.println("<Option>:[-c]  �t�H���[�E�t�H�����[�̏�Ԃ��擾����. *���o�^���[�U�̂݃��[�U�����擾���܂�.");
    System.out.println("         [-h]  �w���v��\������. *���̃I�v�V�����Ɠ����Ɏw�肵���ꍇ�w���v�̂ݕ\������܂�.");
    System.out.println("         [-u] DB�ɓo�^����Ă��郆�[�U�����X�V����(-a,-n,-b �I�v�V�����Ƒg�ݍ��킹�Ďg�p���ĉ�����).");
    System.out.println("         [-a] DB�ɓo�^����Ă���S���[�U�����X�V����.");
    System.out.println("         [-n] DB�ɓo�^����Ă��钆�ŕЎv���������͑��݃t�H���[�̊֌W�ɂ��郆�[�U�����X�V����.");
    System.out.println("         [-b] DB�ɓo�^����Ă��钆�œ����C�E�≏���(�Ўv���E���݃t�H���[�̂ǂ���ł��Ȃ�)�̃��[�U�����X�V����. ");
    System.out.println("");
  }
}
