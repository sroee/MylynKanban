package mylynkanban.servlets;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.eclipse.mylyn.context.core.AbstractContextListener;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.DateRange;
import org.eclipse.mylyn.internal.tasks.core.ITaskListChangeListener;
import org.eclipse.mylyn.internal.tasks.core.TaskContainerDelta;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;

public class GetMylynTasksServlet extends HttpServlet{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1369454849433416215L;

	public void  service (
			  ServletRequest  req, ServletResponse  res )
			  throws ServletException, IOException {
	  PrintStream  out = new PrintStream ( res.getOutputStream ( ) );
	  res.setContentType("application/javascript");
	  out.println ( getTasksData() );
	}
	
//	public String buildDeleteString(AbstractTask task) {
//		return
//			"taskConnector.removeTask(" + task.getTaskId() + ")";
//	}
	
	String getDayOfMonthSuffix(final int n) {
	    if (n >= 11 && n <= 13) {
	        return "th";
	    }
	    switch (n % 10) {
	        case 1:  return "st";
	        case 2:  return "nd";
	        case 3:  return "rd";
	        default: return "th";
	    }
	}
	
	
	
	public String getRelativeDate(Date date) {
		Calendar calendarToday = Calendar.getInstance();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		
		String strRetVal = "";
		if (calendar.get(Calendar.YEAR) != calendarToday.get(Calendar.YEAR)) {
			SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMM d, ''yy");
			// full date
			strRetVal = fullDateFormat.format(date);
		} else if (calendar.get(Calendar.MONTH) != calendarToday.get(Calendar.MONTH)) {
			SimpleDateFormat monthDateFormat = new SimpleDateFormat("MMM d");
			// month, and day in month
			strRetVal = monthDateFormat.format(date);
		} else if (calendar.get(Calendar.WEEK_OF_MONTH) != calendarToday.get(Calendar.WEEK_OF_MONTH)) {
			int dayOfMonth = calendar.get(calendar.DAY_OF_MONTH);
			// day of month
			strRetVal = dayOfMonth + getDayOfMonthSuffix(dayOfMonth);
		} else if (calendar.get(Calendar.DAY_OF_WEEK) != calendarToday.get(Calendar.DAY_OF_WEEK)) {
			// day of week only
			SimpleDateFormat weekDateFormat = new SimpleDateFormat("EEEEEEEEE");
			strRetVal = weekDateFormat.format(date);
		} else {
			strRetVal = "Today";
		}
		
		return strRetVal;
	}
	
	public String buildTaskString(AbstractTask task) {
		String retVal = null;
		Date dueDate = task.getDueDate();
		DateRange range = task.getScheduledForDate();
		Date start = null;
		Date end = null;
		
		String category = null;
		boolean isValidTask = false;
		for (AbstractTaskContainer taskContainer : task.getParentContainers()) {
			if (taskContainer instanceof AbstractTaskCategory) {
				AbstractTaskCategory categoryObj = (AbstractTaskCategory)taskContainer;
				category = categoryObj.getSummary();
				isValidTask = true;
				break;
			}
		}
		
		if (isValidTask) {
			if (range != null) {
				if(range.getStartDate() != null) {
					start = range.getStartDate().getTime();
				}
				if (range.getStartDate() != null) {
					end = range.getStartDate().getTime();
				}
			}
			retVal =  
				"taskConnector.upsertTask(buildTask({" +
					"id:" + task.getTaskId() + 
					",summary:'" + task.getSummary() + 
					"',isCompleted:" + task.isCompleted() +
					",hasContext:" + ContextCore.getContextManager().hasContext(task.getHandleIdentifier()) +
					",isActive:" + task.isActive() +
					((dueDate != null)?(",dueDate:\"" +  getRelativeDate(dueDate) + "\""):"") +
					((start != null)?(",startDate:\"" +  getRelativeDate(start) + "\""):"") +
					((end != null)?(",endDate:\"" +  getRelativeDate(end) + "\""):"") +
					",estimated:" + task.getEstimatedTimeHours() + 
					",category:'" + category + "'" +  
					"}));\n";
		} else {
			retVal = "";
		}
		return retVal;
	}
	
	public String getTasksData() {
		final TaskList taskList = TasksUiPlugin.getTaskList();
//		taskList.addChangeListener(new ITaskListChangeListener() {
//			
//			@Override
//			public void containersChanged(Set<TaskContainerDelta> delta) {
//				for (TaskContainerDelta currDelta : delta) {
//					if (currDelta.getElement() instanceof AbstractTask) {
//						final AbstractTask task = (AbstractTask)currDelta.getElement();
//						switch(currDelta.getKind()) {
//						case ADDED:
//						case CONTENT:
//							System.out.println("added/content detected.");
//							// add/update task
//							disp.asyncExec(new Runnable() {
//								@Override
//								public void run() {
//									browser.execute(buildTaskString(task));
//								}
//							});
//							break;
//						case DELETED:
//						case REMOVED:
//							disp.asyncExec(new Runnable() {
//								@Override
//								public void run() {
//									browser.execute(buildDeleteString(task));
//								}
//							});
//							break;
//						default:
//								break;
//						}
//					}
//				}							
//			}
//		});
//		
//		ContextCore.getContextManager().addListener(new AbstractContextListener() {
//			@Override
//			public void contextActivated(IInteractionContext context) {
//				browser.execute(buildTaskString(taskList.getTask(context.getHandleIdentifier())));
//			}
//			public void contextDeactivated(IInteractionContext context) {
//				browser.execute(buildTaskString(taskList.getTask(context.getHandleIdentifier())));
//			}
//		});
		
		Collection<AbstractTask> tasks = taskList.getAllTasks();
		LinkedList<AbstractTask> reverser = new LinkedList<>();
		for (AbstractTask task: tasks) {
			reverser.push(task);
		}
		StringBuffer tasksStringBuf = new StringBuffer();
		for (AbstractTask task: reverser) {
			tasksStringBuf.append(buildTaskString(task)); 
		}
		return tasksStringBuf.toString();
	}
}
