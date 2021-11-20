package de.thriemer.engine.time;

public class CPUTimerQuery extends TimerQuery {

    private long startTime;

    public CPUTimerQuery(String name) {
        super(name);
    }

    @Override
    public void startQuery() {
        if(timerEnabled){
            startTime=System.nanoTime();
        }
    }

    @Override
    public void stopQuery() {
        if(timerEnabled){
            float msTime =(System.nanoTime()-startTime)/1000000f;
            addTime(msTime);
        }
    }
}
