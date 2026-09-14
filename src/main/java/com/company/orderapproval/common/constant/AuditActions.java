package com.company.orderapproval.common.constant;

public final class AuditActions {
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String PASSWORD_RESET_SUCCESS = "PASSWORD_RESET_SUCCESS";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String ROLE_CREATED = "ROLE_CREATED";
    public static final String ROLE_UPDATED = "ROLE_UPDATED";
    public static final String ROLE_ASSIGNED = "ROLE_ASSIGNED";
    public static final String ROLE_REMOVED = "ROLE_REMOVED";
    public static final String ORGANIZATION_CREATED = "ORGANIZATION_CREATED";
    public static final String ORGANIZATION_UPDATED = "ORGANIZATION_UPDATED";
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_UPDATED = "ORDER_UPDATED";
    public static final String ORDER_CHANGES_REQUESTED = "ORDER_CHANGES_REQUESTED";
    public static final String ORDER_APPROVED = "ORDER_APPROVED";
    public static final String ORDER_REJECTED = "ORDER_REJECTED";
    public static final String ORDER_SUBMITTED = "ORDER_SUBMITTED";
    public static final String ORDER_PENDING = "ORDER_PENDING";
    public static final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";

    private AuditActions() {
    }
}
