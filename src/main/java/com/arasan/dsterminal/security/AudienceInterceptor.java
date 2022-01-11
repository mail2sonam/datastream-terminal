package com.arasan.dsterminal.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AudienceInterceptor implements HandlerInterceptor {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Jwt jwt = (Jwt)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(jwt.getAudience()==null || jwt.getAudience().isEmpty()){
            logger.info("Bad audience-{}",jwt.getAudience());
            throw new AccessDeniedException("Bad Audience");
        }
        String tenantId = request.getHeader("X-TenantID" );
        if(jwt.getAudience().contains(tenantId)){
            return true;
        }
        throw new AccessDeniedException("Unexpected Audience:"+tenantId);
    }
}
