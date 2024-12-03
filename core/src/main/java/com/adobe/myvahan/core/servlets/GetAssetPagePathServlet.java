package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;

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
import org.owasp.encoder.Encode;

import com.apple.learning.logging.core.utility.ExceptionCodes;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplms/getassetpagepaths",
		SLING_SERVLET_METHODS + "=GET" })

public class GetAssetPagePathServlet extends SlingAllMethodsServlet {

	private static final Logger LOG = LoggerFactory.getLogger(GetAssetPagePathServlet.class);
	private static final String CLASS_NAME = GetAssetPagePathServlet.class.getName();
	@Reference
	private ResourceResolverFactory resolverFactory;
	/**
	 * 
	 */
	private static final long serialVersionUID = -4332207652412253871L;

	
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		LOG.info("In side doPost GetAssetPagePathServlet");
		String methodName="doGet";
		try {
			String fullUrl = request.getParameter("assetPath");

			//String finalUrl = fullUrl;
			// Added to fix security issue - Javasecurity:S5131 - Change this code to not reflect user-controlled data.
			LOG.info("GetAssetPagePathServlet:doGEt Before Encode "+fullUrl);
			String finalUrl = Encode.forJava(fullUrl);
			LOG.info("GetAssetPagePathServlet:doGEt After Encode "+finalUrl);
			String dcFormat = "";
			if(null != fullUrl && !fullUrl.isEmpty()) {				
	                if(fullUrl.contains("/content/corplcms/providers/details/document.html")){
	                		fullUrl = fullUrl.replace("/content/corplcms/providers/details/document.html","");
	                }else if(fullUrl.contains("/content/corplcms/providers/details/image.html")){ 
	                		fullUrl = fullUrl.replace("/content/corplcms/providers/details/image.html","");
	                }else if(fullUrl.contains("/content/corplcms/providers/details/video.html")){ 
	                		fullUrl = fullUrl.replace("/content/corplcms/providers/details/video.html","");
	                }
	                
	                String assetPath = fullUrl.substring(0, fullUrl.indexOf('?'));
					Resource PageContentRes = request.getResourceResolver().getResource(assetPath + "/jcr:content/metadata");
					if(null != PageContentRes) {
						ValueMap vm = PageContentRes.getValueMap();
						dcFormat = vm.get("dc:format", String.class) != null? vm.get("dc:format", String.class): "";
						LOG.info("dcFormat "+dcFormat);
						if(null != dcFormat && !dcFormat.isEmpty()) {
							if (dcFormat.split("/")[0].equalsIgnoreCase("image"))
								finalUrl = finalUrl.replace("/content/corplcms/providers/details/document.html", "/content/corplcms/providers/details/image.html");
							else if (dcFormat.split("/")[0].equalsIgnoreCase("video"))
								finalUrl = finalUrl.replace("/content/corplcms/providers/details/document.html", "/content/corplcms/providers/details/video.html");
						}else {
							finalUrl = "";
						}
					}else {
						finalUrl = "";
					}
			}
			LOG.info("dcFormat "+dcFormat+"  "+"finalUrl "+finalUrl);
			if(finalUrl!=null) {
				response.getWriter().write(finalUrl);
			}

		} catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Getting locales and Dam paths", ExceptionCodes.Exception , "Exception occurred: EXCEPTION while getting locales and Dam paths : "+e);
		}

	}

}
