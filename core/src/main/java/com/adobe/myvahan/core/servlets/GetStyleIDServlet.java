package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;
import java.io.IOException;
import java.rmi.ServerException;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.apple.learning.corplcms.core.services.GetStyleID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/getStyleId",
		SLING_SERVLET_METHODS + "=POST" })
public class GetStyleIDServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private final static Logger LOG = LoggerFactory.getLogger(GetStyleIDServlet.class);

	public static final String CLASS_NAME = GetStyleIDServlet.class.getName();

	@Reference

	GetStyleID getStyleID;

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServerException, IOException {

	}

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServerException, IOException {
		String methodName = "doPost";
		JsonObject json = new JsonObject();
		String styleId="";
		LOG.trace(logMethodEntry("Fetching CSS Type List", CLASS_NAME, methodName));
		String type = request.getParameter("type");
		String providerId = request.getParameter("providerId");
		if("provider".equalsIgnoreCase(type)) {
			styleId=getStyleID.getCommonStyleId();
		}else {
			styleId=getStyleID.getProviderStyleId(providerId);
		}
		json.addProperty(styleId, styleId);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(prettyJson);
	}
}
