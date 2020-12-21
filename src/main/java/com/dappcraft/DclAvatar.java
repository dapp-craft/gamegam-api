package com.dappcraft;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DclAvatar {
    public String name;
    public Boolean hasClaimedName;
    public DclAvatarInfo avatar;
}
