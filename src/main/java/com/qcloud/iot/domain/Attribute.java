package com.qcloud.iot.domain;

import java.util.Map;

/**
 * @author matas
 * @date 2019/11/20 11:07
 * @email mataszhang@163.com
 */
public class Attribute {
    private String access;
    private Map<String, String> contentInfo;
    private String contentType;
    private String index;
    private String name;
    private String value;

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public Map<String, String> getContentInfo() {
        return contentInfo;
    }

    public void setContentInfo(Map<String, String> contentInfo) {
        this.contentInfo = contentInfo;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
