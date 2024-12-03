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
import org.json.JSONWriter;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.services.FetchMyItemsDetailsService;
import com.apple.learning.logging.core.utility.ExceptionCodes;


@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/itemsdetails",
  SLING_SERVLET_METHODS + "=POST" })

public class MyItemsDetailsSearch extends SlingAllMethodsServlet {

	private static final long serialVersionUID = -1238148292770358628L;

	private static final String CLASS_NAME = MyItemsDetailsSearch.class.getName();

	private final static Logger LOG = LoggerFactory.getLogger(MyItemsDetailsSearch.class);
	
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
		String methodName = "doPost";
		LOG.debug("In DoPost method");
		JSONWriter writer = null;
		response.setContentType("application/x-json");
		response.setCharacterEncoding("ISO-8859-1");
		writer = getJSONWRITERObject(response);
		
		String providerPath = request.getParameter(Constants.PROVIDERPATH);	
		String courseType = request.getParameter("courseType");	
		String searchType = request.getParameter("searchType");	
		LOG.debug("providerPath:"+providerPath);
		//String data = request.getParameter("data");
		String limit = request.getParameter("limit");
		limit=limit !=null ? limit.trim():null;
		LOG.debug("limit:"+limit);
		String offset = request.getParameter("offset");		
		offset=offset !=null ? offset.trim():null;
		LOG.debug("offset:"+offset);
		try {
			fetchMyItemsDetailsService.fetchItemsDetails(providerPath,limit,offset, writer,
					request.getResourceResolver(),courseType,searchType);
		
		}catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Provider Details Search", ExceptionCodes.Exception , "Exception occurred: EXCEPTION while searching Provider details: "+e);
			
		}
	
		
	}
	
	
	/** 
	 * @param response
	 * @return JSONWriter
	 * @throws IOException
	 */
	protected JSONWriter getJSONWRITERObject(SlingHttpServletResponse response) throws IOException {
		return new JSONWriter(response.getWriter());
	}

}