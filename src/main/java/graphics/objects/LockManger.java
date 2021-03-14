package graphics.objects;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL45.*;

public class LockManger {

    private final List<LockObject> activeFences = new ArrayList<>();
    private final List<LockObject> toRemove = new ArrayList<>();

    public void waitForFence(int beginIndex, int endIndex) {
        toRemove.clear();
        for (LockObject lock : activeFences) {
            if (lock.overlaps(beginIndex, endIndex)) {
                System.out.println("waited: "+lock.waitUntilFree()+" ticks");
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
    private long syncObj;
    private int startIndex;
    private int endIndex;

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
