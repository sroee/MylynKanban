package mylynkanban.servlets;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.mylyn.context.core.AbstractContextListener;
import org.eclipse.mylyn.context.core.ContextChangeEvent;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.context.core.InteractionContext;
import org.eclipse.mylyn.internal.context.core.LocalContextStore;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskCategory;
import org.eclipse.mylyn.internal.tasks.core.AbstractTaskContainer;
import org.eclipse.mylyn.internal.tasks.core.DateRange;
import org.eclipse.mylyn.internal.tasks.core.ITaskListChangeListener;
import org.eclipse.mylyn.internal.tasks.core.TaskContainerDelta;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.monitor.core.InteractionEvent;

public class KanbanTasksAPIWebSocket implements WebSocket.OnTextMessage {
	
	public static String STARTED_DATE_STR = "MYLKANB-START";
	public static String UNDEFINED = "UNDEF";
	
	private Connection m_connection;

	@Override
	public void onClose(int arg0, String arg1) {
		m_connection.close();
	}

	@Override
	public void onOpen(Connection connection) {
		m_connection = connection;
		initTasksComm();
	}

	@Override
	public void onMessage(String arg0) {
		// TODO Auto-generated method stub
		
	}
	
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
	
	public IInteractionContext getTaskContext(AbstractTask task) {
		IInteractionContext context = null;
		if (ContextCore.getContextManager().getActiveContext().getHandleIdentifier() == task.getHandleIdentifier()) {
			context = ContextCore.getContextManager().getActiveContext();
		}
		else if (ContextCore.getContextStore() instanceof LocalContextStore) {
			context = ((LocalContextStore)ContextCore.getContextStore()).loadContext(task.getHandleIdentifier());
		}
		return context;
	}
	public boolean hasContext(AbstractTask task) {
		return ContextCore.getContextManager().hasContext(task.getHandleIdentifier());
	}
	
	public void setTaskStartDate(IInteractionContext context, AbstractTask task) {
		Date startDate = getContextStartDate(context);
		if (startDate == null) {
			task.setAttribute(STARTED_DATE_STR, UNDEFINED);
		} else {
			task.setAttribute(STARTED_DATE_STR, (new Long(startDate.getTime())).toString());
		}
	}
	
	public String buildTaskString(AbstractTask task) {
		String retVal = null;

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
			Date scheduledStart = null;
			Date scheduledEnd = null;
			Date completionDate = task.getCompletionDate();
			Date dueDate = task.getDueDate();
			DateRange scheduledRange = task.getScheduledForDate();
			if (scheduledRange != null) {
				if(scheduledRange.getStartDate() != null) {
					scheduledStart = scheduledRange.getStartDate().getTime();
				}
				if (scheduledRange.getEndDate() != null) {
					scheduledEnd = scheduledRange.getEndDate().getTime();
				}
			}
			Date startDate = null;
			if (task.getAttributes().containsKey(STARTED_DATE_STR)) {
				String strDate = task.getAttribute(STARTED_DATE_STR);
				if (strDate != UNDEFINED) { 
					startDate = new Date(Long.parseLong(task.getAttribute(STARTED_DATE_STR)));
				}
			} else {
				IInteractionContext context = getTaskContext(task);
				setTaskStartDate(context, task);
				String strDate = task.getAttribute(STARTED_DATE_STR);
				if (strDate != UNDEFINED) { 
					startDate = new Date(Long.parseLong(task.getAttribute(STARTED_DATE_STR)));
				}
			}
			
			retVal =  
				"{" +
					"\"id\":" + task.getTaskId() + 
					",\"summary\":\"" + task.getSummary() + 
					"\",\"isCompleted\":" + task.isCompleted() +
					",\"hasContext\":" + hasContext(task) +
					",\"isActive\":" + task.isActive() +
					((dueDate != null)?(",\"dueDate\":\"" +  getRelativeDate(dueDate) + "\""):"") +
					((scheduledStart != null)?(",\"scheduledStartDate\":\"" +  getRelativeDate(scheduledStart) + "\""):"") +
					((scheduledEnd != null)?(",\"scheduledEndDate\":\"" +  getRelativeDate(scheduledEnd) + "\""):"") +
					((completionDate != null)?(",\"completionDate\":\"" +  getRelativeDate(completionDate) + "\""):"") +
					((startDate != null)?(",\"startDate\":\"" +  getRelativeDate(startDate) + "\""):"") +
					",\"estimated\":" + task.getEstimatedTimeHours() + 
					",\"category\":\"" + category + "\"" +  
					"}";
		}
		return retVal;
	}
	
	public String buildUpsertString(Collection<AbstractTask> tasks) {
		StringBuffer tasksStringBuf = new StringBuffer();
		tasksStringBuf.append("{");
		tasksStringBuf.append("\"command\": \"upsert\",");
		tasksStringBuf.append("\"params\": [");
		for (AbstractTask task: tasks) {
			String currTaskString = buildTaskString(task);
			if (currTaskString != null) {
				tasksStringBuf.append(currTaskString + ",");
			}
		}
		tasksStringBuf.deleteCharAt(tasksStringBuf.length() - 1);
		tasksStringBuf.append("]");
		tasksStringBuf.append("}");
		return tasksStringBuf.toString();
	}
	
	public String buildUpsertString(AbstractTask task) {
		StringBuffer taskStringBuf = new StringBuffer();
		taskStringBuf.append("{");
		taskStringBuf.append("\"command\": \"upsert\",");
		taskStringBuf.append("\"params\": [");
		String currTaskString = buildTaskString(task);
		if (currTaskString != null) {
			taskStringBuf.append(currTaskString);
		}
		taskStringBuf.append("]");
		taskStringBuf.append("}");
		return taskStringBuf.toString();
	}
	
	public String buildDeleteString(AbstractTask task) {
		StringBuffer taskStringBuf = new StringBuffer();
		taskStringBuf.append("{");
		taskStringBuf.append("\"command\": \"remove\",");
		taskStringBuf.append("\"params\": ");
		taskStringBuf.append(task.getTaskId());
		taskStringBuf.append("}");
		return taskStringBuf.toString();
	}
	
	public void sendMessageProtected(String message, String errMessage) {
		try {
			m_connection.sendMessage(message);
		} catch(IOException ioe) {
			System.err.println(errMessage);
			ioe.printStackTrace(System.err);
		}
	}
	
	public Date getContextStartDate(IInteractionContext context) {
		List<InteractionEvent> interactions = context.getInteractionHistory();
		if (interactions.size() == 0) {
			return null;
		}
		return context.getInteractionHistory().get(0).getDate();
	}
	
	public void initTasksComm() {
		final TaskList taskList = TasksUiPlugin.getTaskList();
		taskList.addChangeListener(new ITaskListChangeListener() {
			
			@Override
			public void containersChanged(Set<TaskContainerDelta> delta) {
				for (TaskContainerDelta currDelta : delta) {
					if (currDelta.getElement() instanceof AbstractTask) {
						final AbstractTask task = (AbstractTask)currDelta.getElement();
						switch(currDelta.getKind()) {
						case ADDED:
						case CONTENT:
							System.out.println("added/content detected.");
							// add/update task
							sendMessageProtected(buildUpsertString(task), "failed to add/update task:");
							break;
						case DELETED:
						case REMOVED:
							sendMessageProtected(buildDeleteString(task), "failed to remove task:");
							break;
						default:
								break;
						}
					}
				}							
			}
		});
		
		ContextCore.getContextManager().addListener(new AbstractContextListener() {
			boolean isTaskChecked = false;
			
			public void contextChanged(ContextChangeEvent event) {
				super.contextChanged(event);
				IInteractionContext context = event.getContext();
				AbstractTask task = taskList.getTask(context.getHandleIdentifier());
				if (!task.getAttributes().containsKey(STARTED_DATE_STR)) {
					Date startDate = getContextStartDate(context);
					if (startDate != null) {
						task.setAttribute(STARTED_DATE_STR, (new Long(startDate.getTime())).toString());
						isTaskChecked = true;
					} else {
						task.setAttribute(STARTED_DATE_STR, UNDEFINED);
					}
				} else {
					isTaskChecked = true;
				}
			}
			
			@Override
			public void contextActivated(IInteractionContext context) {
				AbstractTask task = taskList.getTask(context.getHandleIdentifier());
				sendMessageProtected(buildUpsertString(
						task), 
						"failed to update activated task:");
			}
			public void contextDeactivated(IInteractionContext context) {
				AbstractTask task = taskList.getTask(context.getHandleIdentifier());
				sendMessageProtected(
						buildUpsertString(task), 
						"failed to update deactivated task:");
				isTaskChecked = false;
			}
		});
		
		Collection<AbstractTask> tasks = taskList.getAllTasks();
		LinkedList<AbstractTask> reverser = new LinkedList<>();
		for (AbstractTask task: tasks) {
			reverser.push(task);
		}
		sendMessageProtected(buildUpsertString(reverser), "failed to initialize tasks list:");
	}

}
