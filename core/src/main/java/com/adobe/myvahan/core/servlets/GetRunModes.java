package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplms/getRunModes",
		SLING_SERVLET_METHODS + "=GET" })
public class GetRunModes extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 12L;
	private final static Logger LOG = LoggerFactory.getLogger(GetRunModes.class);

	@Reference
	private SlingSettingsService slingSettingsService;

	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		JsonObject json = new JsonObject();
		String runmode = "";

		LOG.info("Inside getRunModes servlet:{}", slingSettingsService.getRunModes());
		if (slingSettingsService.getRunModes().contains("dev") == true) {
			runmode = "DEV";
		} else if (slingSettingsService.getRunModes().contains("it") == true) {
			runmode = "IT";
		} else if (slingSettingsService.getRunModes().contains("maintenance") == true) {
			runmode = "MAINTENANCE";
		} else if (slingSettingsService.getRunModes().contains("uat") == true) {
			runmode = "UAT";
		} else if (slingSettingsService.getRunModes().contains("prod") == true) {
			runmode = "PROD";
		} else {
			runmode = "IT";
		}
		LOG.info("runmode is:{}", runmode);

		json.addProperty("runmode", runmode);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(prettyJson);
	}
}
