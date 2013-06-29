package mylynkanban.webserver;

import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

public class WebSocketHandlerImpl extends WebSocketHandler{
	private HashMap<String, WebSocket> m_websocketsMap = new HashMap<String, WebSocket>();
	
	
	public void addWebSocket(String path, WebSocket websocket) {
		m_websocketsMap.put(path, websocket);
	}
	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest req, String protocol) {
		String target = req.getRequestURI();
		System.out.println("trying to handle " + target + " in WebSocketHandlerImpl.");
		return m_websocketsMap.get(target);
	}

}
