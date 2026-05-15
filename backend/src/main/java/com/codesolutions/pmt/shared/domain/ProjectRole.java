package com.codesolutions.pmt.shared.domain;

public enum ProjectRole {
  ADMINISTRATOR,
  MEMBER,
  OBSERVER;

  public boolean canManageMembers() {
    return this == ADMINISTRATOR;
  }

  public boolean canManageTasks() {
    return this == ADMINISTRATOR || this == MEMBER;
  }
}
