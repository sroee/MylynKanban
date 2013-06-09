package mylynkanban.views;


import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import mylynkanban.Activator;

import org.eclipse.mylyn.context.core.AbstractContextListener;
import org.eclipse.mylyn.context.core.ContextCore;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.tasks.core.AbstractTask;
import org.eclipse.mylyn.internal.tasks.core.ITaskListChangeListener;
import org.eclipse.mylyn.internal.tasks.core.TaskContainerDelta;
import org.eclipse.mylyn.internal.tasks.core.TaskList;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */
// TODO: if task contains context - than it is inprog
public class MylynKanbanView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "mylynkanban.views.MylynKanbanView";
	
	private Browser browser;
	private boolean m_isHTMLLoaded = false;
	
	@Override
	public void createPartControl(Composite parent) {
		final Display disp = Display.getCurrent();
		browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		browser.addProgressListener(new ProgressListener() {
			
			@Override
			public void completed(ProgressEvent event) {
				if (!m_isHTMLLoaded) {
					m_isHTMLLoaded = true;
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
										disp.asyncExec(new Runnable() {
											@Override
											public void run() {
												browser.execute(buildTaskString(task));
											}
										});
										break;
									case DELETED:
									case REMOVED:
										disp.asyncExec(new Runnable() {
											@Override
											public void run() {
												browser.execute(buildDeleteString(task));
											}
										});
										break;
									default:
											break;
									}
								}
							}							
						}
					});
					
					ContextCore.getContextManager().addListener(new AbstractContextListener() {
						@Override
						public void contextActivated(IInteractionContext context) {
							browser.execute(buildTaskString(taskList.getTask(context.getHandleIdentifier())));
						}
						public void contextDeactivated(IInteractionContext context) {
							browser.execute(buildTaskString(taskList.getTask(context.getHandleIdentifier())));
						}
					});
					
					Collection<AbstractTask> tasks = taskList.getAllTasks();
					LinkedList<AbstractTask> reverser = new LinkedList<>();
					for (AbstractTask task: tasks) {
						reverser.push(task);
					}
					for (AbstractTask task: reverser) {
						browser.execute(buildTaskString(task)); 
					}
				}
			}
			
			@Override
			public void changed(ProgressEvent event) {
			}
		});

		browser.setUrl(Activator.getAbsoluteURL("src/mylynkanban/views/Kanban.html"));
	}
	
	public String buildDeleteString(AbstractTask task) {
		return
			"taskConnector.removeTask(" + task.getTaskId() + ")";
	}
	public String buildTaskString(AbstractTask task) {
		return 
			"taskConnector.upsertTask(buildTask('" +
				task.getTaskId() + 
				"','" +
				task.getSummary() + 
				"'," +
				task.isCompleted() +
				"," +
				ContextCore.getContextManager().hasContext(task.getHandleIdentifier()) +
				"," +
				task.isActive() + 
				"))";
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}
}