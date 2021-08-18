package graphics.core.objects;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL15;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL45.*;

@Slf4j
public class TimerQuery {
    private static boolean timerEnabled = true;
    private int count = 0;
    private float min = Float.MAX_VALUE;
    private float max = -Float.MAX_VALUE;
    private float avg = 0;
    private final String name;
    private int queryId;
    private final List<Float> gpuTimes = new ArrayList<>();

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
            log.info("Results for {}", name);
            log.info("Min: {}ms, Max: {}ms, AVG: {}ms", format(min, decPlaces), format(max, decPlaces), format(avg / count, decPlaces));
            gpuTimes.sort(Float::compare);
            int p50th = (int) (0.5f * gpuTimes.size());
            int p90th = (int) (0.9f * gpuTimes.size());
            int p95th = (int) (0.95f * gpuTimes.size());
            int p99th = (int) (0.99f * gpuTimes.size());
            int p995th = (int) (0.995f * gpuTimes.size());
            if (!gpuTimes.isEmpty())
                log.info("50%: {}ms, 90%: {}ms, 95%: {}ms, 99%: {}ms, 99.5%: {}ms",
                        format(gpuTimes.get(p50th), decPlaces), format(gpuTimes.get(p90th), decPlaces), format(gpuTimes.get(p95th),
                                decPlaces), format(gpuTimes.get(p99th), decPlaces), format(gpuTimes.get(p995th), decPlaces));
        } else {
            log.info("Timer not enabled");
        }
    }

    private String format(float v, int decimalPlaces) {
        return String.format("%."+decimalPlaces+"f", v);
    }

}
