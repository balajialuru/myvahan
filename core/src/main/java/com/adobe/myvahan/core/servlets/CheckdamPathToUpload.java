package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.services.LocalizationService;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;
import com.day.cq.wcm.msm.api.RolloutManager;


@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/dampath",
		SLING_SERVLET_METHODS + "=POST" })

public class CheckdamPathToUpload extends SlingAllMethodsServlet {
	private final static Logger LOG = LoggerFactory.getLogger(ProviderCreationServlet.class);
	
	private static final String ROLLOUT_CONFIG_DEFAULT = "/etc/msm/rolloutconfigs/corplmscourserollout";
	
	@Reference
	private LocalizationService localizationService;

	@Reference
	private LiveRelationshipManager liveRelationshipManager;

	@Reference
	private RolloutManager rolloutManager;
	
	@Reference
	ResourceResolverFactory resourceResolverFactory;

	
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
		ResourceResolver resourceResolver = null;
		resourceResolver = request.getResourceResolver();
		String uploadPagePath = request.getParameter("uploadPagePath");
		if (resourceResolver.getResource(uploadPagePath) != null) {
			if (!ResourceUtil.isNonExistingResource(resourceResolver.getResource(uploadPagePath))) {
				ValueMap targetResourceMap = resourceResolver.getResource(uploadPagePath + "/jcr:content")
						.adaptTo(ValueMap.class);
				String damPath = targetResourceMap.get("corplcms:damPath", String.class);
				if (damPath != null) {
					response.getWriter().write(damPath);
				}
			}
		} else if (!uploadPagePath.contains(Constants.GLOBALENGLOBAL)) {
			// livecopy creation (Handling the condition where locale page is not present
			// this will arise if at the time of course creation particular locale is not
			// present and after that at the time of uploading some more locales got added
			// to the provider)
			String[] pathArray = uploadPagePath.split(Constants.FORWARDSLASH);
			pathArray[4] = Constants.GLOBAL;
			pathArray[5] = Constants.ENGLOBAL;
			String SourcePath = String.join("/", pathArray);
			try {
				localizationService.createLocaleCopy(SourcePath, uploadPagePath, true, liveRelationshipManager,
						rolloutManager, ROLLOUT_CONFIG_DEFAULT);
			} catch (RepositoryException | WCMException | LoginException e) {
				LOG.error("Exception occured while creating live copy" + e);
			}
			ValueMap targetResourceMap = resourceResolver.getResource(uploadPagePath + "/jcr:content")
					.adaptTo(ValueMap.class);
			String damPath = targetResourceMap.get("corplcms:damPath", String.class);
			if (damPath != null) {
				response.getWriter().write(damPath);
			}
			

		}

	}
}