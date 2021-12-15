package de.thriemer.disguisedphoenix.terrain;

import de.thriemer.graphics.modelinfo.Model;
import lombok.Getter;
import org.joml.Vector3f;

public class Terrain {
    //TODO: collision
    @Getter
    Model model;

    public Terrain(Model model) {
        this.model = model;
    }
}
