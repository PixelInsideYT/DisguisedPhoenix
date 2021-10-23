package de.thriemer.engine.time;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class TimerQuery {

    private static List<TimerQuery> allTimers = new ArrayList<>();

    protected static boolean timerEnabled = true;
    private int count = 0;
    private float min = Float.MAX_VALUE;
    private float max = -Float.MAX_VALUE;
    private float avg = 0;
    private final String name;
    private final List<Float> gpuTimes = new ArrayList<>();

    protected void addTime(float msTime) {
        gpuTimes.add(msTime);
        min = Math.min(min, msTime);
        max = Math.max(max, msTime);
        avg += msTime;
        count++;
    }

    protected TimerQuery(String name) {
        this.name = name;
        allTimers.add(this);
    }

    public abstract void startQuery();

    public abstract void stopQuery();

    public void printResults() {
        if (timerEnabled && !gpuTimes.isEmpty()) {
            int decPlaces = 3;
            log.info("Results for {}", name);
            log.info("Min: {}ms, Max: {}ms, AVG: {}ms", format(min, decPlaces), format(max, decPlaces), format(avg / count, decPlaces));
            gpuTimes.sort(Float::compare);
            int p50th = (int) (0.5f * gpuTimes.size());
            int p90th = (int) (0.9f * gpuTimes.size());
            int p95th = (int) (0.95f * gpuTimes.size());
            int p99th = (int) (0.99f * gpuTimes.size());
            int p995th = (int) (0.995f * gpuTimes.size());
            log.info("50%: {}ms, 90%: {}ms, 95%: {}ms, 99%: {}ms, 99.5%: {}ms",
                    format(gpuTimes.get(p50th), decPlaces), format(gpuTimes.get(p90th), decPlaces), format(gpuTimes.get(p95th),
                            decPlaces), format(gpuTimes.get(p99th), decPlaces), format(gpuTimes.get(p995th), decPlaces));
        }
    }

    private String format(float v, int decimalPlaces) {
        return String.format("%." + decimalPlaces + "f", v);
    }

    public static void printAllResults() {
        allTimers.forEach(TimerQuery::printResults);
    }

}
