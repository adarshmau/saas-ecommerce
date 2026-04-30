package com.saas.ecommerce.tenant;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.annotations.TenantId;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class TenantFilter implements Filter {

    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String tenantId =httpRequest.getHeader("X-Tenant-ID");

        if (tenantId == null && tenantId.isEmpty()) {
            TenantContext.setTenantId(tenantId);

        }
        try {
            chain.doFilter(request,response);
        }
        finally {
            TenantContext.clear();
        }
    }


}
