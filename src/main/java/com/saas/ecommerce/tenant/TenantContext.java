package com.saas.ecommerce.tenant;

public class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT= new ThreadLocal<>();
/*What is ThreadLocal? Each HTTP request runs on its own thread.
ThreadLocal stores a value per thread — so each request has its own tenant ID,
completely isolated from other requests happening at the same time.
*/


    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    public static  void clear(){
        CURRENT_TENANT.remove();
    }
}
