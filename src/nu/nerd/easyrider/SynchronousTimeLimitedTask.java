package nu.nerd.easyrider;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.BooleanSupplier;

import org.bukkit.Bukkit;

// ----------------------------------------------------------------------------
/**
 * A synchronous Bukkit task that imposes a strict limit on its run time in any
 * one tick and automatically re-schedules itself to run in the next tick until
 * the work is done.
 *
 * The time limit is taken from {@link EasyRider#CONFIG}.
 */
public class SynchronousTimeLimitedTask implements Runnable {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     * 
     * Sets a empty queue of processing steps.
     */
    public SynchronousTimeLimitedTask() {
        _steps = new ArrayDeque<BooleanSupplier>();
    }

    // ------------------------------------------------------------------------
    /**
     * Construct a SynchronousTimeLimitedTask from a list of steps to be
     * performed.
     *
     * @param steps the list of steps, in the order they should be performed.
     *        Each step should return true if there is more work to be done.
     *
     */
    public SynchronousTimeLimitedTask(Queue<BooleanSupplier> steps) {
        _steps = steps;
    }

    // ------------------------------------------------------------------------
    /**
     * Add a processing step.
     * 
     * @param step the step to perform.
     */
    public void addStep(BooleanSupplier step) {
        _steps.add(step);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if all task steps have been performed.
     * 
     * @return true if all task steps have been performed.
     */
    public boolean isFinished() {
        return _steps.isEmpty();
    }

    // ------------------------------------------------------------------------
    /**
     * @see java.lang.Runnable#run()
     *
     *      Call process() until there is no more work to be done or the time
     *      limit is reached. Reschedule this task to run in the next tick if
     *      processing was paused due to the time limit.
     */
    @Override
    public void run() {
        long start = System.nanoTime();
        long elapsed;
        boolean more;
        do {
            more = process();
            elapsed = System.nanoTime() - start;
        } while (more && elapsed < EasyRider.CONFIG.SCAN_TIME_LIMIT_MICROS * 1000);

        if (more) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(EasyRider.PLUGIN, this);
        }

        EasyRider.PLUGIN.getLogger().info("Time limited tasks ran for " + elapsed * 0.001 + " microseconds.");
    }

    // ------------------------------------------------------------------------
    /**
     * Run the next step and return true if it has more work to do or if there
     * are subsequent steps still to be done.
     *
     * @return true if there is more work to be done.
     */
    protected boolean process() {
        BooleanSupplier step = _steps.peek();
        if (step == null) {
            return false;
        }
        if (step.getAsBoolean()) {
            return true;
        } else {
            _steps.remove();
            return !isFinished();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Steps, in sequence.
     */
    protected Queue<BooleanSupplier> _steps;
} // class SynchronousTimeLimitedTask