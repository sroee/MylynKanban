package mylynkanban.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.osgi.framework.BundleContext;

public class BundleResourceHandler extends HandlerWrapper  {
	private String m_path;
	private BundleContext m_bundleContext;
	
	public String getPath() {
		return m_path;
	}
	public void setPath(String path) {
		m_path = path;
	}
	
	public BundleContext getBundleContext() {
		return m_bundleContext;
	}
	public void setBundleContext(BundleContext bundleContext) {
		this.m_bundleContext = bundleContext;
	}
	
	@Override
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		System.out.println("trying to handle " + target + " in BundleResourceHandler.");
		if (m_path == null) {
			System.err.println("Must add in bundle path, before calling handle of mylynkanban.webserver.BundleResourceHandler");
			throw new ServletException("Must add in bundle path.");
		}
		
		if (m_bundleContext == null) {
			System.err.println("Must add in bundle context, before calling handle of mylynkanban.webserver.BundleResourceHandler");
			throw new ServletException("Must add in bundle context.");
		}
		
		
		System.out.println(target);
		
		String contentType = detectContentType(target);
		if (contentType == null) {
			System.out.println("mylynkanban.webserver.BundleResourceHandler: Unknown content type for " + target + ", passing on.");
		} else {
			response.setContentType(contentType);

			InputStream resourceContent = getResourceContent(target);
			
			if (resourceContent != null) {
				response.setStatus(HttpServletResponse.SC_OK);
		        baseRequest.setHandled(true);
		        
		        byte[] buffer = new byte[4096 * 2];
		        int lengthRead = 0;
		        while ((lengthRead = resourceContent.read(buffer)) != -1)
					response.getOutputStream().write(buffer, 0, lengthRead);
				resourceContent.close();
			}
		}
	}
	
	private URL getResourceURL(String target) {
		return m_bundleContext.getBundle().getEntry(m_path + target);
	}
	private InputStream getResourceContent(String target) throws IOException{
		URL resourceInternalURL = getResourceURL(target);
		return resourceInternalURL.openStream();
	}
	
	private String getExtention(String resourceName) {
		String[] tokens = resourceName.split("\\.(?=[^\\.]+$)");
		if (tokens.length > 1) {
			return tokens[1];
		} else {
			return null;
		}
	}
	private String detectContentType(String target) {
		String extention = getExtention(target);
		String retVal = null;
		if (extention.equals("html")) {
			retVal = "text/html";
		} else if (extention.equals("js")) {
			retVal = "application/javascript";
		} else if (extention.equals("css")) {
			retVal = "text/css";
		} else if (extention.equals("png")) {
			retVal = "image/png";
		} else if (extention.equals("ico")) {
			retVal = "image/x-icon";
		}
		return retVal;
	}
}
