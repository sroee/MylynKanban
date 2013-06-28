package mylynkanban;

import java.io.File;
import java.io.FileReader;

import mylynkanban.servlets.GetMylynTasksServlet;
import mylynkanban.webserver.BundleResourceHandler;
import mylynkanban.webserver.ServletsHandler;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "MylynKanban"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private Server server;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		server = new Server(9999);
		Handler bundleResourceHandler = new BundleResourceHandler();
		((BundleResourceHandler)bundleResourceHandler).setBundleContext(context);
		((BundleResourceHandler)bundleResourceHandler).setPath("/webroot");

		Handler servletHandler = new ServletsHandler();
		((ServletsHandler)servletHandler).addServlet("/service/get_mylyn_tasks.js", new GetMylynTasksServlet());
		
		HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] {servletHandler, bundleResourceHandler});
		server.setHandler(handlerList);
		server.start();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		server.stop();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	public static String getAbsoluteURL(String path){
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		if (!BundleUtility.isReady(bundle)) {
			return null;
		}
		String loc = bundle.getLocation();
		loc = loc.substring(loc.indexOf("file:"), loc.length()).concat(path);
		
		return loc;
	}
}
