package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;
import java.io.IOException;
import java.rmi.ServerException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.apple.learning.course.container.multilms.services.ProviderLMSMapService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/getregionlocale",
		SLING_SERVLET_METHODS + "=POST" })
public class GetRegionsLocalesServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private final static Logger LOG = LoggerFactory.getLogger(GetRegionsLocalesServlet.class);

	public static final String CLASS_NAME = GetRegionsLocalesServlet.class.getName();

	private Session userSession;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;
	
	@Reference
	ProviderLMSMapService providerLMSMapService;

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServerException, IOException {

	}
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServerException, IOException {
		String methodName = "doPost";
		JsonObject json = new JsonObject();
		String localeBasePath = "/content/cq:tags/corplcms/locale";
		LOG.trace(logMethodEntry("Fetching CSS Type List", CLASS_NAME, methodName));
		try {
			json.addProperty("en_Global", "GLOBAL");
			ResourceResolver resourceResolver = null;
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);
			userSession = resourceResolver.adaptTo(Session.class);
			Node localeBaseNode = userSession.getNode(localeBasePath);
			NodeIterator baseIterator = localeBaseNode.getNodes();
			while (baseIterator != null && baseIterator.hasNext()) {

				Node countryNode = baseIterator.nextNode();
				NodeIterator countryIterator = countryNode.getNodes();
				while (countryIterator != null && countryIterator.hasNext()) {

					Node localeNode = countryIterator.nextNode();
					String localeCode = localeNode.getName();
					String regionCode = providerLMSMapService.getRegionFromLocale(localeCode);
					if(!localeCode.toLowerCase().contains("global"))
					json.addProperty(localeCode, regionCode);

				}

			}
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String prettyJson = gson.toJson(json);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(prettyJson);
		} catch (LoginException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
	}
}