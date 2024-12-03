package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.course.container.multilms.services.ProviderLMSMapService;
import com.apple.learning.logging.core.utility.ExceptionCodes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplms/getpagelocalepaths",
		SLING_SERVLET_METHODS + "=GET" })

public class GetLocalePagePathServlet extends SlingAllMethodsServlet {

	private static final Logger LOG = LoggerFactory.getLogger(GetLocalePagePathServlet.class);
	private static final String CLASS_NAME = GetLocalePagePathServlet.class.getName();
	@Reference
	private ResourceResolverFactory resolverFactory;
	/**
	 * 
	 */
	private static final long serialVersionUID = -4332207652412253871L;

	@Reference
	ProviderLMSMapService providerLMSMapService;
	
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		LOG.info("In side doPost GetLocalePagePathServlet");
		String methodName="doGet";
		try {
			String globalPagePath = request.getParameter("globalPagePath");
			Resource PageContentRes = request.getResourceResolver().getResource(globalPagePath + "/jcr:content");
			ValueMap vm = PageContentRes.getValueMap();
			String globalDamPath = vm.get("corplcms:damPath", String.class) != null
					? vm.get("corplcms:damPath", String.class)
					: "/content/dam/corplcms";
			JsonObject localeJson = new JsonObject();
			localeJson.addProperty("en_Global", globalDamPath);
			String locales = request.getParameter("locales");
			String localesArr[] = locales.split(",");
			String[] globalPathSplits = globalDamPath.split("/");
			String dampath = String.join("/", Arrays.copyOfRange(globalPathSplits, 0, 6));
			String restDampath = String.join("/", Arrays.copyOfRange(globalPathSplits, 8, globalPathSplits.length));
			for (String locale : localesArr) {
				String val = "";
				String countryCode = locale.split("_")[1];
				String regionFromLocaleString = providerLMSMapService.getRegionFromLocale(locale);
				LOG.info("thee regionFromLocaleString name is " + regionFromLocaleString);
				val = dampath + "/" + regionFromLocaleString + "/" + countryCode + "/"
						+ locale + "/" + restDampath;
				localeJson.addProperty(locale, val);

			}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String prettyJson = gson.toJson(localeJson);
			LOG.info("localeJson:" + prettyJson);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(prettyJson);
		} catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Getting locales and Dam paths", ExceptionCodes.Exception , "Exception occurred: EXCEPTION while getting locales and Dam paths : "+e);
		}

	}

}
