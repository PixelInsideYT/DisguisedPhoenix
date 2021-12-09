package de.thriemer.disguisedphoenix.terrain.generator;

import com.google.gson.annotations.SerializedName;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

public class BiomeInformation {

    String name;
    Vector2f[] ccwPolygon;
    String[] models;
    public Vector3f color;


    public float getBiomeDistance(Vector2f samplePoint){
        boolean isInsidePolygon = true;
        float minDistance = Float.MAX_VALUE;
        float sum =0;
        for(int i=0;i<ccwPolygon.length;i++){
            int next = (i+1)%ccwPolygon.length;
            Vector2f start = ccwPolygon[i];
            Vector2f end = ccwPolygon[next];
            if(!isLeftOfLine(start,end,samplePoint))isInsidePolygon=false;
            minDistance=Math.min(minDistance,calculateMinDistance(start,end,samplePoint));
            sum+=0.5f*(start.x*end.y-end.x*start.y);
        }
        if(sum<=0){
            System.out.println(name+" is not CCW! "+sum);
        }
        if(!isInsidePolygon)minDistance=-minDistance;
        return minDistance;
    }


    private boolean isLeftOfLine(Vector2f start,Vector2f finish,Vector2f vector2f){
        Vector2f normal = new Vector2f(finish).sub(start);
        float temp = normal.x;
        normal.x=-normal.y;
        normal.y=temp;
        return normal.dot(new Vector2f(vector2f).sub(start))>0;
    }

    private float calculateMinDistance(Vector2f start,Vector2f finish, Vector2f point){
        Vector2f lineVec = new Vector2f(finish).sub(start);
        float lengthSquared= lineVec.lengthSquared();
        if(lengthSquared==0)return start.distance(point);
        float t = Math.max(0, Math.min(1,new Vector2f(point).sub(start).dot(lineVec)/lengthSquared));
        Vector2f projection = new Vector2f(start).add(lineVec.mul(t));
        return projection.distance(point);
    }

}
