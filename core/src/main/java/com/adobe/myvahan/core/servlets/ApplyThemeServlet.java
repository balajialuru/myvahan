package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.ArrayList;

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

import com.apple.learning.corplcms.lms.services.CreateThemeClientLibService;
import com.apple.learning.corplcms.util.CommonUtil;

import lombok.Synchronized;

@Component(service = Servlet.class, immediate = true, enabled = true, property = {
		SLING_SERVLET_PATHS + "=/bin/corplcms/theme/apply", SLING_SERVLET_METHODS + "=POST" })
public class ApplyThemeServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = -1238148292770358629L;
	private static final Logger LOG = LoggerFactory.getLogger(ApplyThemeServlet.class);
	private transient ResourceResolver resolver;

	@Reference
	private transient CreateThemeClientLibService clientLibService;

	@Reference
	private transient ResourceResolverFactory resolverFactory;

	String themeClientlibPath;

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

	@Override
	@Synchronized
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		String methodName = "doPost";
		LOG.debug("In  method {}", methodName);

		ArrayList<String> inUseProviderClientlibList = new ArrayList<String>();
		String themePath = request.getParameter("themePath");
		try {
			resolver = CommonUtil.getServiceResourceResolver(null, resolverFactory);
			Resource themeResource = resolver.getResource(themePath);
			clientLibService.createThemeClientLib(inUseProviderClientlibList, themeResource,resolver);
			response.setStatus(200);
		} catch (Exception e) {
			LOG.error("Exception while applying theme:{}", e);
			response.setStatus(500);
		}
	}

}