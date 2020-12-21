package com.dappcraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DclUserMetadata {
    public List<DclAvatar> avatars;
}
