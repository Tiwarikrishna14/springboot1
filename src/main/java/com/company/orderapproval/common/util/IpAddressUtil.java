package com.company.orderapproval.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpAddressUtil {

    private IpAddressUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
