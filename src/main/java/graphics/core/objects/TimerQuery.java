package graphics.core.objects;

import org.lwjgl.opengl.GL15;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL45.*;

public class TimerQuery {
    public static boolean timerEnabled = true;
    public int count = 0;
    public float min = Float.MAX_VALUE;
    public float max = -Float.MAX_VALUE;
    public float avg = 0;
    String name;
    int queryId;
    private List<Float> gpuTimes = new ArrayList<>();

    public TimerQuery(String name) {
        this.name = name;
    }

    public void startQuery() {
        if (timerEnabled) {
            queryId = glGenQueries();
            glBeginQuery(GL_TIME_ELAPSED, queryId);
        }
    }

    public void waitOnQuery() {
        if (timerEnabled) {
            glEndQuery(GL_TIME_ELAPSED);
            int[] done = new int[]{0};
            while (done[0] == 0) {
                glGetQueryObjectiv(queryId, GL_QUERY_RESULT_AVAILABLE, done);
            }
            long[] elapsedTime = new long[1];
            glGetQueryObjectui64v(queryId, GL15.GL_QUERY_RESULT, elapsedTime);
            float msTime = elapsedTime[0] / 1000000.0f;
            gpuTimes.add(msTime);
            min = Math.min(min, msTime);
            max = Math.max(max, msTime);
            avg += msTime;
            count++;
        }
    }

    public void printResults() {
        if (timerEnabled) {
            int decPlaces = 3;
            System.out.println("Results for " + name);
            System.out.println("Min: " + format(min, decPlaces) + "ms, Max: " + format(max, decPlaces) + "ms, AVG: " + format(avg / count, decPlaces) + "ms");
            gpuTimes.sort((f1, f2) -> Float.compare(f1, f2));
            int p50th = (int) (0.5f * gpuTimes.size());
            int p90th = (int) (0.9f * gpuTimes.size());
            int p95th = (int) (0.95f * gpuTimes.size());
            int p99th = (int) (0.99f * gpuTimes.size());
            int p995th = (int) (0.995f * gpuTimes.size());
            if (gpuTimes.size() > 0)
                System.out.println(" 50%: " + format(gpuTimes.get(p50th), decPlaces) + " 90%: " + format(gpuTimes.get(p90th), decPlaces) + " 95%: " + format(gpuTimes.get(p95th), decPlaces) + " 99%: " + format(gpuTimes.get(p99th), decPlaces) + " 99,5%: " + format(gpuTimes.get(p995th), decPlaces));
        } else {
            System.out.println("Timer not enabled");
        }
    }

    private String format(float v, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f", v);
    }

}
