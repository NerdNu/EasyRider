package nu.nerd.easyrider;

// ----------------------------------------------------------------------------
/**
 * Limits the rate of some task.
 */
public class RateLimiter {
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param coolDownMillis minimum elapsed time between task runs.
     */
    public RateLimiter(long coolDownMillis) {
        _coolDownMillis = coolDownMillis;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the cool down time between task runs, in milliseconds.
     * 
     * @param coolDownMillis minimum elapsed time between task runs.
     */
    public void setCoolDownMillis(long coolDownMillis) {
        _coolDownMillis = coolDownMillis;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the cool down time between task runs, in milliseconds.
     * 
     * @return the cool down time between task runs, in milliseconds.
     */
    public long getCoolDownMillis() {
        return _coolDownMillis;
    }

    // ------------------------------------------------------------------------
    /**
     * Run the task if the cool down time has elapsed.
     * 
     * @param task the task to run.
     * @return true if the task was run.
     */
    public boolean run(Runnable task) {
        long now = System.currentTimeMillis();
        if (now - _lastRunTime >= _coolDownMillis) {
            _lastRunTime = now;
            task.run();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Minimum elapsed time between task runs.
     */
    protected long _coolDownMillis;

    /**
     * Time stamp of the last task run.
     */
    protected long _lastRunTime;
} // class RateLimiter