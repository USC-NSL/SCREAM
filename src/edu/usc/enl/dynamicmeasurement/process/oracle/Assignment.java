package edu.usc.enl.dynamicmeasurement.process.oracle;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.taskthread.LoadTrafficTaskMethod;
import edu.usc.enl.dynamicmeasurement.data.trace.FilterTraceMapping;
import edu.usc.enl.dynamicmeasurement.process.EpochPacket;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 12/13/13
 * Time: 6:53 PM
 */
abstract class Assignment implements Runnable {

    protected final OracleResource.TaskRecord taskRecord;
    protected final double resourceShareStep;
    protected final double lowThreshold;
    protected final int epochSize;
    protected final long startTime;
    protected final FilterTraceMapping mapping;

    Assignment(OracleResource.TaskRecord taskRecord, double lowThreshold, int epochSize, double resourceShareStep, long startTime, FilterTraceMapping mapping) {
        this.taskRecord = taskRecord;
        this.lowThreshold = lowThreshold;
        this.epochSize = epochSize;
        this.resourceShareStep = resourceShareStep;
        this.startTime = startTime;
        this.mapping = mapping;
    }

    protected void runTask(MultiSwitchTask task, int step) {
        task.setStep(step);
        task.getUpdateTaskMethod().run();

        LoadTrafficTaskMethod processTaskMethod = task.getProcessTaskMethod();
        processTaskMethod.setEpoch(new EpochPacket(epochSize * step + startTime, step));
        processTaskMethod.run();

        task.getReportTaskMethod().run();
        task.updateStats();
    }
}
