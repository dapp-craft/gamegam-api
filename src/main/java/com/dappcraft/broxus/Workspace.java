package com.dappcraft.broxus;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Workspace {
    public String id;
    public String name;
    public Boolean isDefault;
}
