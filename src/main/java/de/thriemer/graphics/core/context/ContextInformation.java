package de.thriemer.graphics.core.context;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContextInformation {

    private int width;
    private int height;
    private float aspectRatio;

    public ContextInformation(int width, int height) {
       updateSize(width,height);
    }

    public ContextInformation updateSize(int width, int height) {
        this.width = width;
        this.height = height;
        this.aspectRatio=(float)width/(float) height;
        return this;
    }

}
