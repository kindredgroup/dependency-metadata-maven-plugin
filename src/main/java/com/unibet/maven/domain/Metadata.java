package com.unibet.maven.domain;

import org.codehaus.jackson.annotate.JsonProperty;

public class Metadata {
    @JsonProperty
    public int formatVersion;

    @JsonProperty
    public boolean applyOnPreviousVersions;

    @JsonProperty
    public String message;

    @JsonProperty
    public boolean fail;
}
