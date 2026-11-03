package com.shop.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shop.storage")
public class StorageProperties {
    /**
     * Local filesystem directory to persist uploaded files
     */
    private String localDir = "./uploads";

    /**
     * URL mapping prefix exposed via WebMvc resource handlers
     */
    private String urlPrefix = "/uploads/";

    public String getLocalDir() {
        return localDir;
    }

    public void setLocalDir(String localDir) {
        this.localDir = localDir;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }
}
