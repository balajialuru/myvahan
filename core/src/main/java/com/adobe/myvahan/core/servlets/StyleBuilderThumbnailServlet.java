package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.HtmlResponse;
import com.adobe.granite.xss.XSSAPI;
import com.apple.learning.corplcms.core.services.ProviderAction;
import com.apple.learning.logging.core.utility.ExceptionCodes;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.i18n.I18n;

/**
 * The Class ProviderServlet.
 */
@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/styleThemesThumbnail",
		SLING_SERVLET_METHODS + "=POST" })

public class StyleBuilderThumbnailServlet extends SlingAllMethodsServlet {

	private static final String PROVIDERS = "providers";

	private static final String BASE = "base";

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant CLASS_NAME. */
	private static final String CLASS_NAME = StyleBuilderThumbnailServlet.class.getName();

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(StyleBuilderThumbnailServlet.class);

	private transient List<ProviderAction> providerActions;

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

		Session session = request.getResourceResolver().adaptTo(Session.class);

		final XSSAPI xssAPI = request.getResourceResolver().adaptTo(XSSAPI.class);
		final HtmlResponse res = new HtmlResponse(xssAPI, i18n, request.getLocale());
		ResourceResolver resolver = request.getResourceResolver();
		String path = request.getParameter("path");
		String baseThemePath = "/content/dam/corplcms/providers/shared/styleBuilderIcons/baseThemes/manualThumbnail.jpg";
		String providerThemePath = "/content/dam/corplcms/providers/shared/styleBuilderIcons/providerThemes/manualThumbnail.jpg";

		LOG.info("styleThemesThumbnailJcrPath:{}", path);

		try {
			Resource baseThemeThumbnailResource = resolver.getResource(baseThemePath);
			Resource providerThemeThumbnailResource = resolver.getResource(providerThemePath);
			Resource themeJcResource = resolver.getResource(path);
			if (providerThemeThumbnailResource != null && themeJcResource != null && path.contains(PROVIDERS)) {
				Node providerThemeNode = providerThemeThumbnailResource.adaptTo(Node.class);
				Node providerThemeJcrNode = themeJcResource.adaptTo(Node.class);
				JcrUtil.copy(providerThemeNode, providerThemeJcrNode, null);
			}
			if (baseThemeThumbnailResource != null && themeJcResource != null && path.contains(BASE)) {
				Node baseThemeNode = baseThemeThumbnailResource.adaptTo(Node.class);
				Node baseThemeJcrNode = themeJcResource.adaptTo(Node.class);
				JcrUtil.copy(baseThemeNode, baseThemeJcrNode, null);
			}
			session.save();

		} catch (Exception e) {
			e.printStackTrace();
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Style Builder Properties", ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while saving style builder properties : " + e);
			res.send(response, false);
		}

	}

	/**
	 * Bind.
	 *
	 * @param providerActionService the provider action service
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
	 * @param providerActionService the provider action service
	 */
	protected void unbind(final ProviderAction providerAction) {
		providerActions.remove(providerAction);
	}

}