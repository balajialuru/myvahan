package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.core.services.AddLocaleFilterService;
import com.day.cq.wcm.api.Page;


@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/addLocalesServlet",
		SLING_SERVLET_METHODS + "=POST" })

public class AddLocaleFilters extends SlingAllMethodsServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2157140082232222199L;

	private final static Logger LOG = LoggerFactory.getLogger(AddLocaleFilters.class);

	@Reference
	private AddLocaleFilterService addLocaleFilterService;
	
	
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
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		LOG.debug("In DoPost method");
		String currentPagePath = request.getParameter("currentPath");
		Resource currentPageResource=request.getResourceResolver().getResource(currentPagePath.replace(".html", ""));
		if(null!=currentPageResource) {
			Page currentPage = currentPageResource.adaptTo(Page.class);
			if(currentPage.getPath() != null) {
				addLocaleFilterService.addLocales(currentPage.getPath());
			}
		}
		response.getWriter().write("success");
	}
}