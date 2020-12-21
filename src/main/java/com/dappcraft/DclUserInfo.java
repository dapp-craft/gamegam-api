package com.dappcraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DclUserInfo {
    public DclUserMetadata metadata;
}
