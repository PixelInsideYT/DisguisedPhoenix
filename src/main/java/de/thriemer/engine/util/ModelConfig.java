package de.thriemer.engine.util;

import java.util.Map;

public class ModelConfig extends Object {

    public String modelFilePath;
    public String relativePath;
    public String relativeColliderPath;
    public Map<String, String> wobbleInfo;
    public float modelHeight;

    public ModelConfig(String modelFilePath, String relativePath, String relativeColliderPath, Map<String, String> wobbleInfo, float modelHeight) {
        this.modelFilePath = modelFilePath;
        this.relativePath = relativePath;
        this.relativeColliderPath = relativeColliderPath;
        this.wobbleInfo = wobbleInfo;
        this.modelHeight = modelHeight;
    }

    public boolean equals(Object other) {
        if (other instanceof ModelConfig)
            return relativePath.equals(((ModelConfig) other).relativePath);
        return false;
    }

}
