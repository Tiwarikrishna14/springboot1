package com.company.orderapproval.common.exception;

public class ConflictException extends RuntimeException {

        private final boolean canReactivate;

    public ConflictException(String message) {
        this(message, false);
    }

    public ConflictException(String message, boolean canReactivate) {
        super(message);
        this.canReactivate = canReactivate;
    }

    public boolean isCanReactivate() {
        return canReactivate;
    }
    
    public record ConflictData(boolean canReactivate) {}


}
