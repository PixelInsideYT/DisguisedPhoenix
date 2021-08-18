package engine.time;

public class CPUTimerQuery extends TimerQuery {

    private long startTime;

    public CPUTimerQuery(String name) {
        super(name);
    }

    @Override
    public void startQuery() {
        if(timerEnabled){
            startTime=System.currentTimeMillis();
        }
    }

    @Override
    public void stopQuery() {
        if(timerEnabled){
            float msTime =(System.currentTimeMillis()-startTime);
            addTime(msTime);
        }
    }
}
