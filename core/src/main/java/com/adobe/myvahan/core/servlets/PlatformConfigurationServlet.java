package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.core.services.PlatformConfigurationService;
import com.apple.learning.logging.core.utility.ExceptionCodes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplms/getPlatformConfiguration",
		SLING_SERVLET_METHODS + "=GET" })

public class PlatformConfigurationServlet extends SlingAllMethodsServlet {

	private static final Logger LOG = LoggerFactory.getLogger(PlatformConfigurationServlet.class);
	private static final String CLASS_NAME = PlatformConfigurationServlet.class.getName();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Reference
	PlatformConfigurationService platformConfigurationService;
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		LOG.info("In side doPost PlatformConfigurationServlet");
		String methodName="doGet";
		try {
			JsonObject configJson = new JsonObject();
			configJson.addProperty("consumerName", platformConfigurationService.getConsumerName());
			configJson.addProperty("draftThemeProvider", platformConfigurationService.getDraftThemeProvider());
			configJson.addProperty("liveThemeProvider", platformConfigurationService.getLiveThemeProvider());
			configJson.addProperty("draftCourseContainerPath", platformConfigurationService.getDraftCourseContainerPath());
			configJson.addProperty("liveCourseContainerPath", platformConfigurationService.getLiveCourseContainerPath());
			configJson.addProperty("previewOption", platformConfigurationService.getPreviewOption());
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String prettyJson = gson.toJson(configJson);
			LOG.info("configJson:" + prettyJson);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(prettyJson);
		} catch (Exception e) {
			e.printStackTrace();
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Getting locales and Dam paths", ExceptionCodes.Exception , "Exception occurred: EXCEPTION while getting Platform Configuration : "+e);
		}

	}

}
