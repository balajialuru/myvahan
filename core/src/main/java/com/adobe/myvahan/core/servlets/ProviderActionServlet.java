package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.HtmlResponse;
import com.adobe.granite.xss.XSSAPI;
import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.models.ProviderData;
import com.apple.learning.corplcms.core.services.ProviderAction;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import com.apple.learning.logging.core.utility.ExceptionCodes;
import com.day.cq.i18n.I18n;

/**
 * The Class ProviderServlet.
 */
@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/providerAction",
		SLING_SERVLET_METHODS + "=POST" })

public class ProviderActionServlet extends SlingAllMethodsServlet {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant CLASS_NAME. */
	private static final String CLASS_NAME = ProviderActionServlet.class.getName();

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ProviderActionServlet.class);
	
	
	private transient List<ProviderAction> providerActions;
	
	@Reference
	private ResourceResolverFactory resolverFactory;
	
	@Override
	protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {

		LOG.info("In DoGet method");
		doPost(request, response);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {
		String methodName = "doPost";
		I18n i18n = new I18n(request);
		ResourceResolver serviceResourceResolver = null;
		final XSSAPI xssAPI = request.getResourceResolver().adaptTo(XSSAPI.class);
		final HtmlResponse res = new HtmlResponse(xssAPI, i18n, request.getLocale());
		try {
			final ProviderData providerData = new ProviderActionRequestHelper().populateProviderData(request);
			String providerType = request.getParameter(Constants.PROVIDER_TYPE) != null
					? request.getParameter(Constants.PROVIDER_TYPE) :Constants.SIMPLE_PROVIDER;
			final String action = providerData.getOperation()!=null?Constants.PROVIDER_CREATION_ACTION:Constants.PROVIDER_UPDATE_ACTION;
			 ProviderAction providerAction = getProviderActionService(providerType, action);
			serviceResourceResolver = resolverFactory.getServiceResourceResolver(null);
			final String providerId = providerAction.doAction(providerData, serviceResourceResolver);
			if(providerData.getSecureContentData()!=null) {
			LOG.info(" "+providerData.getSecureContentData().getOperation());
			if(Constants.SECUREENABLE.equals(providerData.getSecureContentData().getOperation())) {
				 providerAction = getProviderActionService(providerType, Constants.PROVIDER_CREATION_ACTION);
				 providerData.setProviderId(providerId);
				 String internalProviderPath = Constants.CONTENTPARENTPATH+ Constants.FORWARDSLASH
							+ providerId + Constants.FORWARDSLASH + providerId;
				 providerData.getSecureContentData().setInternalProviderPath(internalProviderPath);
				 providerData.getSecureContentData().setOperation(Constants.EXISTINGPROVIDERENABLE);
				 providerAction.doAction(providerData, serviceResourceResolver);
			}
			
			if(Constants.SECUREUPDATE.equals(providerData.getSecureContentData().getOperation())) {
				 providerAction = getProviderActionService(providerType, Constants.PROVIDER_UPDATE_ACTION);
				 providerData.setProviderId(providerId);
				 providerData.getSecureContentData().setOperation(Constants.UPDATEEXISTING);
				 providerAction.doAction(providerData, serviceResourceResolver);
			}
		
			}
			LOG.info("providerData.getActionName()"+providerData.getActionName());
			if(Constants.SECUREPROVIDERUPDATE.equals(providerData.getActionName())) {
				 providerAction = getProviderActionService(providerType, Constants.PROVIDER_UPDATE_ACTION);
				 providerData.setProviderId(providerId);
				 providerData.setPath(Constants.CONTENTPARENTPATH);
					LOG.info("providerData.getPath()"+providerData.getPath());
				 providerData.setActionName(Constants.UPDATETOINTERNAL);
				 providerAction.doAction(providerData, serviceResourceResolver);
			}
			
			final String homeUrl = Constants.PROVIDERURL + providerData.getPath();
			final String detailsUrl = Constants.PROVIDERDETAILURL + providerData.getPath() + Constants.FORWARDSLASH
					+ providerId + Constants.FORWARDSLASH + providerId;
			res.onCreated(providerData.getPath() + Constants.FORWARDSLASH + providerData.getTitle());
			res.setStatus(201, i18n.get(Constants.PROVIDERCREATED));
			res.setTitle(i18n.get(Constants.PROVIDERCREATED));
			res.setDescription(i18n.get(Constants.PROVIDERSUCCESSDESCRIPTION));
			res.addRedirectLink(xssAPI.getValidHref(homeUrl), i18n.get(Constants.DONE));
			res.addLink(Constants.PROJECCT_ADMIN_CREATEPROJECT_EDIT, xssAPI.getValidHref(detailsUrl),
					i18n.get(Constants.OPENPROVIDER));
			res.send(response, true);
		} catch (Exception e) {
			e.printStackTrace();
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Provider Creation", ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while creating Provider : " + e);
			res.send(response, false);
		}

	}
	public ProviderAction getProviderActionService(final String providerType, final String ationName) {
		ProviderAction providerAction = null;
		LOG.debug("providerType"+ providerType);
		LOG.debug("ationName"+ ationName);
		for (final ProviderAction prAction : providerActions) {
			LOG.debug(" prAction.getClass().getName()"+ prAction.getClass().getName());
			LOG.debug(" prAction.isAllowedAction(providerType, ationName)"+ prAction.isAllowedAction(providerType, ationName));
			if (prAction.isAllowedAction(providerType, ationName)) {
				providerAction = prAction;
				break;
			}
		}
		return providerAction;
	}

	/**
	 * Bind.
	 *
	 * @param providerActionService
	 *            the provider action service
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void bind(final ProviderAction providerAction) {
		
		LOG.info("bind Provider Action Called");
		if (providerActions == null) {
			providerActions = new ArrayList<ProviderAction>();
		}
		providerActions.add(providerAction);
	}

	/**
	 * Unbind.
	 *
	 * @param providerActionService
	 *            the provider action service
	 */
	protected void unbind(final ProviderAction providerAction) {
		providerActions.remove(providerAction);
	}
	
}
