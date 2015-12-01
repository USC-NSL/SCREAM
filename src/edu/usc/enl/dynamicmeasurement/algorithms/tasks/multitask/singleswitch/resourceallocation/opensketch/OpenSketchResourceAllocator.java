package edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.resourceallocation.opensketch;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.resourceallocation.AllocationTaskView;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.resourceallocation.MultiTaskResourceControl;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.resourceallocation.TaskRecord2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.SSAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm.SSCountMinMultiSwitch3;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/15/2014
 * Time: 5:18 AM
 */
public class OpenSketchResourceAllocator extends MultiTaskResourceControl {
    protected final List<TaskRecord2> taskRecords;
    private double width;
    private double widthss;
    private int capacity;
    private int currentUsedShare;

    public OpenSketchResourceAllocator(Element element, MonitorPoint monitorPoint) {
        taskRecords = new LinkedList<>();
        Map<String, Element> properties = Util.getChildrenProperties(element, "Property");

        capacity = monitorPoint.getCapacity();
        width = Double.parseDouble(properties.get("Width").getAttribute(ConfigReader.PROPERTY_VALUE));
        widthss = Double.parseDouble(properties.get("Widthss").getAttribute(ConfigReader.PROPERTY_VALUE));
        currentUsedShare = 0;

    }

    @Override
    public void allocate() {

    }

    @Override
    public boolean addTask(AllocationTaskView task) {
        int share = getShare(task);

        if (currentUsedShare + share <= capacity) {
            task.setResourceShare(share);
            taskRecords.add(new TaskRecord2(task, -taskRecords.size()));
            currentUsedShare += share;
            return true;
        } else {
            return false;
        }
    }

    protected int getShare(AllocationTaskView task) {
        int wildcardNum = task.getTask().getFilter().getWildcardNum();
        Task2.TaskImplementation implementation = task.getTask().getUser().getImplementation();
        int depth = AbstractHierarchicalCountMinSketch.FIXED_DEPTH;
        if (implementation instanceof SSAlgorithm) {
            int share = (int) Math.ceil(widthss * depth * (wildcardNum - ((int) (Math.log(widthss * depth) / Math.log(2))) + 2));
            return share * ((SSCountMinMultiSwitch3) implementation).getCCSize();

        } else {
            return (int) Math.ceil(width * depth * (wildcardNum - ((int) (Math.log(width * depth) / Math.log(2))) + 2));
        }

    }

    @Override
    public void removeTask(AllocationTaskView task) {
        task.setResourceShare(0);
        for (Iterator<TaskRecord2> iterator = taskRecords.iterator(); iterator.hasNext(); ) {
            TaskRecord2 taskRecord = iterator.next();
            if (taskRecord.getTask().equals(task)) {
                iterator.remove();
                currentUsedShare -= getShare(task);
                break;
            }
        }
    }
}
