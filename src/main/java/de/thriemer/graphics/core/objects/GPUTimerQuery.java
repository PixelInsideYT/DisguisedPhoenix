package de.thriemer.graphics.core.objects;

import de.thriemer.engine.time.TimerQuery;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL15;

import static org.lwjgl.opengl.GL45.*;

@Slf4j
public class GPUTimerQuery extends TimerQuery {

    private int queryId;

    public GPUTimerQuery(String name) {
       super(name);
    }

    public void startQuery() {
        if(queryId!=0)
            collectResult();
        if (timerEnabled) {
            queryId = glGenQueries();
            glBeginQuery(GL_TIME_ELAPSED, queryId);
        }
    }

    public void stopQuery() {
        if (timerEnabled) {
            glEndQuery(GL_TIME_ELAPSED);
        }
    }

    private void collectResult(){
        int[] done = new int[]{0};
        while (done[0] == 0) {
            glGetQueryObjectiv(queryId, GL_QUERY_RESULT_AVAILABLE, done);
        }
        long[] elapsedTime = new long[1];
        glGetQueryObjectui64v(queryId, GL15.GL_QUERY_RESULT, elapsedTime);
        float msTime = elapsedTime[0] / 1000000.0f;
        addTime(msTime);
        queryId=0;
    }

}
