package graphics.objects;

import org.lwjgl.opengl.GL15;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL45.*;

public class TimerQuery {
    private List<Float> gpuTimes = new ArrayList<>();
    String name;
    int queryId;
    public int count = 0;
    public float min = Float.MAX_VALUE;
    public float max = -Float.MAX_VALUE;
    public float avg = 0;

    public TimerQuery(String name) {
        this.name = name;
    }

    public void startQuery() {
        queryId = glGenQueries();
        glBeginQuery(GL_TIME_ELAPSED, queryId);
    }

    public void waitOnQuery(float ... floats) {
        glEndQuery(GL_TIME_ELAPSED);
        int[] done = new int[]{0};
        while (done[0] == 0) {
            glGetQueryObjectiv(queryId, GL_QUERY_RESULT_AVAILABLE, done);
        }
        long[] elapsedTime = new long[1];
        glGetQueryObjectui64v(queryId, GL15.GL_QUERY_RESULT, elapsedTime);
        float msTime = elapsedTime[0] / 1000000.0f;
        if(floats.length>0){
            msTime/=floats[0];
        }
        gpuTimes.add(msTime);
        min = Math.min(min, msTime);
        max = Math.max(max, msTime);
        avg += msTime;
        count++;
    }

    public void printResults(float... scaler) {
        int decPlaces = 3;
        float scale = scaler.length>0?scaler[0]:1;
        System.out.println("Results for " + name);
        System.out.println("Min: " + format(min, decPlaces,scale) + "ms, Max: " + format(max, decPlaces,scale) + "ms, AVG: " + format(avg / count, decPlaces,scale) + "ms");
        gpuTimes.sort((f1, f2) -> Float.compare(f1, f2));
        int p50th = (int) (0.5f * gpuTimes.size());
        int p90th = (int) (0.9f * gpuTimes.size());
        int p95th = (int) (0.95f * gpuTimes.size());
        int p99th = (int) (0.99f * gpuTimes.size());
        int p995th = (int) (0.995f * gpuTimes.size());
        if (gpuTimes.size() > 0)
            System.out.println(" 50%: " + format(gpuTimes.get(p50th), decPlaces,scale) + " 90%: " + format(gpuTimes.get(p90th), decPlaces,scale) + " 95%: " + format(gpuTimes.get(p95th), decPlaces,scale) + " 99%: " + format(gpuTimes.get(p99th), decPlaces,scale) + " 99,5%: " + format(gpuTimes.get(p995th), decPlaces,scale));
    }

    private String format(float v, int decimalPlaces,float scaler) {
        return String.format("%." + decimalPlaces + "f", v*scaler);
    }

}
