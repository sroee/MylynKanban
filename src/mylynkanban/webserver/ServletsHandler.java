package mylynkanban.webserver;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;

public class ServletsHandler extends HandlerWrapper {
	private HashMap<String, HttpServlet> m_servletsMap = new HashMap<String, HttpServlet>();
	
	
	public void addServlet(String path, HttpServlet servlet) {
		m_servletsMap.put(path, servlet);
	}	
	public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		HttpServlet servlet = m_servletsMap.get(target);
		if (servlet != null) {
			servlet.service(request, response);
			baseRequest.setHandled(true);
		}
	}
}
