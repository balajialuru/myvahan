package com.apple.learning.corplcms.core.servlets;

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

import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.services.RollOutService;

@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/rollout",
  SLING_SERVLET_METHODS + "=POST" })
public class RolloutServlet extends SlingAllMethodsServlet {
	
	private static final long serialVersionUID = -1238148292770358628L;

	private final static Logger LOG = LoggerFactory.getLogger(RolloutServlet.class);
	
	@Reference
	RollOutService rolloutService;
	
    
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
    	 String globalPagePath = request.getParameter(Constants.GLOBALPGEPATH); 
    	 String[] targetPathsArray = request.getParameterValues(Constants.TARGETPATHS);
    	 String[] intermediatePathsArray = request.getParameterValues(Constants.INTERMEDIATEPATHS);
    	 rolloutService.rollOutToRegions(globalPagePath, targetPathsArray, intermediatePathsArray);
    
    }

}
