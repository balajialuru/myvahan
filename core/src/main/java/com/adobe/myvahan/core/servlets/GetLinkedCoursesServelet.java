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
import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.services.FetchMyItemsDetailsService;
import com.apple.learning.logging.core.utility.ExceptionCodes;

@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/linkedCourseSearch",
  SLING_SERVLET_METHODS + "=GET" })
public class GetLinkedCoursesServelet extends SlingAllMethodsServlet {
	
	private static final long serialVersionUID = -1238148292770358628L;

	private static final String CLASS_NAME = GetLinkedCoursesServelet.class.getName();

	private final static Logger LOG = LoggerFactory.getLogger(GetLinkedCoursesServelet.class);
	
	@Reference
	FetchMyItemsDetailsService fetchMyItemsDetailsService;

	
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
		String methodName="doPost";
		try {
			LOG.debug("before calling method");
			String providerPath = request.getParameter(Constants.PROVIDERPATH);
			String courseTitle = request.getParameter(Constants.SEARCHTEXT);
			String jsonStr=fetchMyItemsDetailsService.fetchLinkedCourses(providerPath, courseTitle, request.getResourceResolver());
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(jsonStr);
			LOG.debug("after calling method");
		
		}catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Provider Details Search", ExceptionCodes.Exception , "Exception occurred: EXCEPTION while searching Provider details: "+e);
			
		}
		
	}

}
