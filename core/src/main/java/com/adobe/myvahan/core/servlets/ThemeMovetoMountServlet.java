package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodExit;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.core.services.CorpLcmsThemeBuilderService;
import com.apple.learning.corplcms.lms.services.ThemeClientlibPushService;
import com.apple.learning.corplcms.util.CommonUtil;

import lombok.Synchronized;

@Component(service = Servlet.class, immediate = true, enabled = true, property = {
		SLING_SERVLET_PATHS + "=/bin/corplcms/theme/pushToLms", SLING_SERVLET_METHODS + "=POST" })
public class ThemeMovetoMountServlet extends SlingAllMethodsServlet {

	private static final String THEME_CLIENTLIB_MOVE_TO_MOUNT_SERVLET = "Theme Clientlib move to mount servlet";
	/** The Constant CLASS_NAME. */
	private static final String CLASS_NAME = ThemeMovetoMountServlet.class.getName();
	private static final Logger LOG = LoggerFactory.getLogger(ThemeMovetoMountServlet.class);
	private static final String SLASH = "/";
	private static final String BASE_LOCALE = "baseLocale";
	private static final String CORPCC_USED_PROVIDERS = "corpcc:usedProviders";
	private static final String THEME_TYPE = "themeType";
	private static final String JCR_CONTENT = "jcr:content";
	private static final long serialVersionUID = 1L;
	private transient ResourceResolver resolver;

	@Reference
	private transient ResourceResolverFactory resolverFactory;

	@Reference
	private transient ThemeClientlibPushService themeClienlibPushService;

	@Reference
	private transient CorpLcmsThemeBuilderService corpLcmsThemeBuilderService;

	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		LOG.info("In DoGet method");
		doPost(request, response);
	}

	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	@Synchronized
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		String methodName = "doPost";
		LOG.trace(logMethodEntry(THEME_CLIENTLIB_MOVE_TO_MOUNT_SERVLET, CLASS_NAME, methodName));
		try {
			String themeClientlib = "";
			String themeType = "";
			String themeName = "";
			String providerThemeClientlibPath = corpLcmsThemeBuilderService.getproviderThemeClientlibPath() + SLASH;
			String baseThemeClientlibPath = corpLcmsThemeBuilderService.getbaseThemeClientlibPath() + SLASH;
			resolver = CommonUtil.getServiceResourceResolver(null, resolverFactory);
			Session session = resolver.adaptTo(Session.class);
			String themePath = request.getParameter("themePath");
			Resource themeResource = resolver.getResource(themePath);
			ArrayList<String> inUseProviderClientlibList = new ArrayList<String>();
			themeType = themeResource.getChild(JCR_CONTENT).getValueMap().get(THEME_TYPE, String.class);
			if (BASE_LOCALE.equalsIgnoreCase(themeType)) {
				themeClientlib = baseThemeClientlibPath;
				// Get Locale folder name
				themeName = themeResource.getParent().getName() + SLASH + themeResource.getName();
				inUseProviderClientlibList.add(themeClientlib + themeName);
			} else {
				themeClientlib = providerThemeClientlibPath;
				themeName = themeResource.getName();
				if (themeResource.getChild(JCR_CONTENT).getValueMap().containsKey(CORPCC_USED_PROVIDERS)) {
					String[] inUseProviders = themeResource.getChild(JCR_CONTENT).getValueMap()
							.get(CORPCC_USED_PROVIDERS, String[].class);
					for (String providerName : inUseProviders) {
						inUseProviderClientlibList.add(themeClientlib + "" + providerName);
					}
				}
			}
			moveToMount(inUseProviderClientlibList, session);
			Node jcrContentNode = themeResource.getChild(JCR_CONTENT).adaptTo(Node.class);
			if (jcrContentNode != null) {
				jcrContentNode.setProperty("pushToMountStatus", "completed");
			}
			session.save();
			response.setStatus(200);

		} catch (Exception e) {
			LOG.error("Exception while applying theme:{}", e);
			response.setStatus(500);
		}
		LOG.trace(logMethodExit(THEME_CLIENTLIB_MOVE_TO_MOUNT_SERVLET, CLASS_NAME, methodName));
	}

	/**
	 * Move Theme Clientlib to mount path
	 * 
	 * @param inUseProviderClientlibList
	 * @param session
	 */
	private void moveToMount(ArrayList<String> inUseProviderClientlibList, Session session) {
		String methodName = "moveToMount";
		LOG.trace(logMethodEntry(THEME_CLIENTLIB_MOVE_TO_MOUNT_SERVLET, CLASS_NAME, methodName));
		LOG.info("inUseProviderClientlibListThemeMovetoMountServlet:{}", inUseProviderClientlibList);
		if (!inUseProviderClientlibList.isEmpty()) {
			themeClienlibPushService.pushToMount(
					inUseProviderClientlibList.toArray(new String[inUseProviderClientlibList.size()]), session);
		}
		LOG.trace(logMethodExit(THEME_CLIENTLIB_MOVE_TO_MOUNT_SERVLET, CLASS_NAME, methodName));
	}
}
