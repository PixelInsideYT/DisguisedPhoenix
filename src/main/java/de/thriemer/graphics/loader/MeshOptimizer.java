package de.thriemer.graphics.loader;

import org.joml.Vector3f;

import java.util.*;

public class MeshOptimizer {
    //TODO: rework mesh optimizer, huge runtimes
    public static final int MaxSizeVertexCache = 32;

    public static void optimize(List<Vector3f> modelVerticies, List<Integer> modelIndicies) {
        List<Triangle> triangles = new ArrayList<>();
        for (int i = 0; i < modelIndicies.size(); i += 3) {
            triangles.add(new Triangle(modelVerticies.get(modelIndicies.get(i)), modelVerticies.get(modelIndicies.get(i + 1)), modelVerticies.get(modelIndicies.get(i + 2))));
        }
        modelIndicies.clear();
        List<Triangle> sortedTriangles = new ArrayList<>();
        List<Vector3f> LRUCache = new ArrayList<>();
        while (triangles.size() > 0) {
            Triangle.clearNumTrisMap();
            triangles.sort((t1, t2) -> Float.compare(t2.getScore(LRUCache, triangles), t1.getScore(LRUCache, triangles)));
            Triangle picked = triangles.get(0);
            //add triangle to cache
            LRUCache.addAll(0, Arrays.asList(picked.verticies));
            while (LRUCache.size() > MaxSizeVertexCache) {
                LRUCache.remove(LRUCache.size() - 1);
            }
            sortedTriangles.add(picked);
            triangles.remove(picked);
        }
        List<Vector3f> sortedVerticies = new ArrayList<>();
        for (Triangle t : sortedTriangles) {
            for (Vector3f v : t.verticies) {
                if (modelVerticies.contains(v)) {
                    sortedVerticies.add(v);
                    modelVerticies.remove(v);
                }
            }
        }
        modelVerticies.clear();
        modelVerticies.addAll(sortedVerticies);
        for (Triangle t : sortedTriangles) {
            for (Vector3f v : t.verticies)
                modelIndicies.add(modelVerticies.indexOf(v));
        }
        Triangle.clearNumTrisMap();
    }
}

class Triangle {
    private static final float FindVertexScore_CacheDecayPower = 1.5f;
    private static final float FindVertexScore_LastTriScore = 0.75f;
    private static final float FindVertexScore_ValenceBoostScale = 2.0f;
    private static final float FindVertexScore_ValenceBoostPower = 0.5f;
    private static final Map<Vector3f, Integer> activeTris = new HashMap<>();

    Vector3f[] verticies;

    public Triangle(Vector3f... verticies) {
        this.verticies = verticies;
    }

    public static void clearNumTrisMap() {
        Triangle.activeTris.clear();
    }

    public float getScore(List<Vector3f> lruCache, List<Triangle> notRenderd) {
        float score = 0;
        for (Vector3f v : verticies)
            score += getVertexScore(v, lruCache, notRenderd);
        return score;
    }

    private float getVertexScore(Vector3f v, List<Vector3f> lruCache, List<Triangle> notRenderd) {
        float Score = 0;
        int numActiveTris = getNumActiveTriangles(notRenderd, v);
        if (numActiveTris == 0) {
            // No tri needs this vertex!
            return -1.0f;
        }
        int CachePosition = lruCache.indexOf(v);
        if (CachePosition < 0) {
            // Vertex is not in FIFO cache - no score.
        } else {
            if (CachePosition < 3) {
                // This vertex was used in the last triangle,
                // so it has a fixed score, whichever of the three
                // it's in. Otherwise, you can get very different
                // answers depending on whether you add
                // the triangle 1,2,3 or 3,1,2 - which is silly.
                Score = FindVertexScore_LastTriScore;
            } else if (CachePosition < MeshOptimizer.MaxSizeVertexCache) {
                //assert (CachePosition < MaxSizeVertexCache);
                // Points for being high in the cache.
                float Scaler = 1.0f / (MeshOptimizer.MaxSizeVertexCache - 3);
                Score = 1.0f - (CachePosition - 3) * Scaler;
                Score = (float) Math.pow(Score, FindVertexScore_CacheDecayPower);
            }
        }
        // Bonus points for having a low number of tris still to
        // use the vert, so we get rid of lone verts quickly.
        float ValenceBoost = (float) Math.pow(numActiveTris, -FindVertexScore_ValenceBoostPower);
        Score += FindVertexScore_ValenceBoostScale * ValenceBoost;
        return Score;
    }

    private int getNumActiveTriangles(List<Triangle> notRenderd, Vector3f toFind) {
        if (activeTris.containsKey(toFind)) return activeTris.get(toFind);
        int activeTris = 0;
        for (Triangle t : notRenderd) {
            if (t.hasVertex(toFind)) activeTris++;
        }
        Triangle.activeTris.put(toFind, activeTris);
        return activeTris;
    }

    public boolean hasVertex(Vector3f v) {
        for (int i = 0; i < verticies.length; i++) {
            if (v.equals(verticies[i])) return true;
        }
        return false;
    }
}