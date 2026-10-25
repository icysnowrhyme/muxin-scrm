package com.muxin.scrm.security.handler;

import com.muxin.scrm.core.web.ResultCode;
import com.muxin.scrm.core.web.WebResponseWriter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 无权限访问处理器
 *
 * @author MUXIN Team
 * @since 2.0.0
 */
public class MyAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        WebResponseWriter.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
    }

}
