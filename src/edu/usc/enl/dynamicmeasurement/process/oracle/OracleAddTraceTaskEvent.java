package edu.usc.enl.dynamicmeasurement.process.oracle;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.data.trace.InputTrace;
import edu.usc.enl.dynamicmeasurement.model.event.AddTraceTaskEvent;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 12/14/13
 * Time: 6:43 AM
 */
public class OracleAddTraceTaskEvent extends AddTraceTaskEvent {
    private Map<Task2, InputTrace> taskInputTraceMap;

    public OracleAddTraceTaskEvent(Element element) {
        super(element);
    }

    public void setInputTraceMap(Map<Task2, InputTrace> taskInputTraceMap) {
        this.taskInputTraceMap = taskInputTraceMap;
    }

    @Override
    public void run() throws Exception {
        Task2 task = getTask();
        InputTrace inputTrace = mapping.getInputTrace(task.getFilter());
        task.setTraceReader(inputTrace.getTaskTraceReader(task, getEpoch(), false));
        handler.addTask(task, getEpoch());
        if (taskInputTraceMap != null) {
            taskInputTraceMap.put(task, inputTrace);
        }
    }
}
