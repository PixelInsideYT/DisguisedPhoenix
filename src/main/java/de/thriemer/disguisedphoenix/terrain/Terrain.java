package de.thriemer.disguisedphoenix.terrain;

import de.thriemer.graphics.modelinfo.Model;
import lombok.Getter;
import org.joml.Vector3f;

public class Terrain {
    //TODO: collidision
    @Getter
    Model model;
    Vector3f min;
    Vector3f max;

    public Terrain(Model model) {
        this.model = model;
    }
}
