package com.joshlong.mogul.api;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class MogulSecurityContexts {

    private final MogulService mogulService;

    public MogulSecurityContexts(MogulService mogulService) {
        this.mogulService = mogulService;
    }

    public record MogulPrincipal(Mogul mogul) implements Principal {

        @Override
        public String getName() {
            return mogul().username();
        }

    }


    public UsernamePasswordAuthenticationToken install(Long mogulId) {
        var mogul = mogulService.getMogulById(mogulId);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(new MogulPrincipal(mogul), null,
                AuthorityUtils.NO_AUTHORITIES);
        var securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        var context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        return authentication;
    }

}
