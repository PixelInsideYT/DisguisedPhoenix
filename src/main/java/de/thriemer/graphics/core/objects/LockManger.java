package de.thriemer.graphics.core.objects;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL45.*;

@Slf4j
public class LockManger {

    private final List<LockObject> activeFences = new ArrayList<>();
    private final List<LockObject> toRemove = new ArrayList<>();

    public void waitForFence(int beginIndex, int endIndex) {
        toRemove.clear();
        for (LockObject lock : activeFences) {
            if (lock.overlaps(beginIndex, endIndex)) {
                log.error("waited: {} ticks",lock.waitUntilFree());
            }
            if (lock.isDone()) toRemove.add(lock);
        }
        activeFences.removeAll(toRemove);
    }

    public void addFence(int beginIndex, int endIndex) {
        long syncObj = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        LockObject bufferLock = new LockObject(syncObj, beginIndex, endIndex);
        activeFences.add(bufferLock);
    }


}

class LockObject {
    private boolean fenceReturned;
    private final long syncObj;
    private final int startIndex;
    private final int endIndex;

    public LockObject(long syncObj, int startIndex, int endIndex) {
        this.syncObj = syncObj;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    public boolean overlaps(int begin, int end) {
        return (begin >= startIndex && begin <= endIndex) || (end >= startIndex && end <= endIndex);
    }

    public int waitUntilFree() {
        int waittime = 0;
        while (!isDone()) {
            waittime++;
        }
        return waittime;
    }

    public void delete() {
        glDeleteSync(syncObj);
    }

    public boolean isDone() {
        //avoid double check for removal and in while loop
        return fenceReturned || checkFence();
    }


    private boolean checkFence() {
        int answer = glClientWaitSync(syncObj, GL_SYNC_FLUSH_COMMANDS_BIT, 1);
        fenceReturned = answer == GL_ALREADY_SIGNALED || answer == GL_CONDITION_SATISFIED;
        return fenceReturned;
    }

}
