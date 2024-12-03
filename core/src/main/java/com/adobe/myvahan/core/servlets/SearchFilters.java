package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.constants.Constants;
import com.day.cq.tagging.TagManager;

@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/providerfilters",
  SLING_SERVLET_METHODS + "=POST" })
public class SearchFilters extends SlingAllMethodsServlet{
 
	private static final long serialVersionUID = -1238148292770358628L;

	private final static Logger LOG = LoggerFactory.getLogger(SearchFilters.class);
	
	TagManager tagManager ;
    
	
    
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
       
     try
     {
    	 
    	 ResourceResolver resourceResolver = request.getResourceResolver();
    	 JSONObject obj=new JSONObject();
    	 
    	 String providerPath = request.getParameter(Constants.PROVIDERPATH);     	 
    	 LOG.debug("providerPath:"+providerPath);
    	 
    	 Resource contentResource = resourceResolver.getResource(providerPath);		
    	 LOG.debug("contentResource:"+contentResource);
    	 
    	 ValueMap valueMap = contentResource.adaptTo(ValueMap.class); 
    	 TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
    	 
    	 for (String filter : valueMap.get(Constants.SEARCHFILTERS, String[].class)) {
    		 
        	 	com.day.cq.tagging.Tag searchTag = tagManager.resolve(filter); 
        	 	if(searchTag!=null){
        	 		obj.put( searchTag.getName(), true);
        	 	}
 			
 		}       
            //Get the JSON formatted data    
         String jsonData = obj.toString();
           
            //Return the JSON formatted data
        response.getWriter().write(jsonData);
     }
     catch(Exception e)
     {
    	 LOG.error("Error reading search filters. ",e);
     }
   } 
 
}