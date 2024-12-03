package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.apple.learning.corplcms.core.models.ProviderFilters;
import com.apple.learning.corplcms.core.services.FilterBasedProviderDetailsService;
import com.apple.learning.logging.core.utility.ExceptionCodes;


@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/filtersSearch",
  SLING_SERVLET_METHODS + "=POST" })

public class FiltersBasedProviderDetails extends SlingAllMethodsServlet {

	private static final long serialVersionUID = -1238148292770358628L;

	private static final String CLASS_NAME = FiltersBasedProviderDetails.class.getName();

	private final static Logger LOG = LoggerFactory.getLogger(FiltersBasedProviderDetails.class);
	
	@Reference
	FilterBasedProviderDetailsService filterBasedProviderDetailsService;

	
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
		ProviderFilters providerfilters = new ProviderFilters();
		writer = getJSONWRITERObject(response);
		
		String provider = request.getParameter(Constants.PROVIDERPATH);
		String searchText = request.getParameter(Constants.SEARCHTEXT);
		String[] countries = request.getParameterValues("selectedCountriesArray");
		String[] locales = request.getParameterValues("selectedLocalesArray");
		String[] status = request.getParameterValues("selectedStatusesArray");
		String[] cmsTags = request.getParameterValues("selectedCMSTagsArray");
		String modifiedStartDate = request.getParameter("modifiedStartDate");
		String modifiedEndDate = request.getParameter("modifiedEndDate");
		String pushToLmsStartDate = request.getParameter("pushToLmsStartDate");
		String pushToLmsEndDate = request.getParameter("pushToLmsEndDate");
		String createdByUser = request.getParameter("createdByUser");
		String courseType = request.getParameter("courseType");
		String searchType = request.getParameter("searchType");
		String[] setTypes =  request.getParameterValues("selectedSetTypeArray");
		String[] types =  request.getParameterValues("selectedTypeArray");
		//for pagination
		String limit = request.getParameter("limit");		
		String offset = request.getParameter("offset");		
		
		LOG.debug("offset:"+offset);
		LOG.debug("limit:"+limit);
		LOG.debug("providerPath:"+provider);
		LOG.debug("countries:"+countries);	
		LOG.debug("cms tags:"+cmsTags);	
		LOG.debug("locales:"+locales);	
		LOG.debug("status:"+status);
		LOG.debug("startDate:"+modifiedStartDate);
		LOG.debug("enddate:"+modifiedEndDate);
		LOG.debug("setTypes:"+setTypes);
		LOG.debug("Types:"+types);
		
		providerfilters.setProvider(provider);
		providerfilters.setCountries(countries);
		providerfilters.setLocales(locales);
		providerfilters.setCmsTags(cmsTags);
		providerfilters.setStatus(status);
		providerfilters.setSearchText(searchText);
		providerfilters.setModifiedStartDate(modifiedStartDate);
		providerfilters.setModifiedEndDate(modifiedEndDate);
		providerfilters.setPushToLmsStartDate(pushToLmsStartDate);
		providerfilters.setPushToLmsEndDate(pushToLmsEndDate);
		providerfilters.setSearchType(searchType);
		providerfilters.setSetType(setTypes);
		providerfilters.setType(types);
		
		if(createdByUser!=null){
			createdByUser = createdByUser.trim();
		}
		Pattern userNamePattern = Pattern.compile("[a-zA-Z0-9_]*");
		Matcher userNameMatcher = userNamePattern.matcher(createdByUser);
		if(userNameMatcher.matches()){
			providerfilters.setCreatedByUser(createdByUser);
		}
		
		try {
			LOG.debug("before calling method");
			filterBasedProviderDetailsService.fetchFilteredProviderDetails(providerfilters, courseType, limit, offset, writer,
					request.getResourceResolver());
			
			LOG.debug("after calling method");
		
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