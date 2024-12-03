package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
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
import com.day.cq.i18n.I18n;

/**
 * The Class ProviderServlet.
 */
@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/stylebuilderAction",
		SLING_SERVLET_METHODS + "=POST" })

public class StyleBuilderActionServlet extends SlingAllMethodsServlet {

	private static final String CORPCC_PROVIDER_LIST = "corpcc:providerList";

	private static final String JCR_DESCRIPTION = "jcr:description";

	private static final String CQ_JS_STYLES = "cq:jsStyles";

	private static final String CQ_CSS_STYLES = "cq:cssStyles";

	private static final String CQ_STYLE_GROUPS = "cq:styleGroups";

	private static final String JCR_TITLE = "jcr:title";

	private static final String JCR_CONTENT = "jcr:content";

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The Constant CLASS_NAME. */
	private static final String CLASS_NAME = StyleBuilderActionServlet.class.getName();

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(StyleBuilderActionServlet.class);

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
		LOG.info("requestInStylebuilderParam:{}", request.getParameterMap());
		String providerPath = request.getHeader("Referer").split("item=")[1];
		String title = request.getParameter("./jcr:title");
		String desc = request.getParameter("./jcr:description");
		String[] providerList = request.getParameterValues(CORPCC_PROVIDER_LIST);
		String[] cssStyles = request.getParameterValues("./cq:styleGroups/cq:cssStyles");
		String[] jsStyles = request.getParameterValues("./cq:styleGroups/cq:jsStyles");

		LOG.info("providerPath:{}", providerPath);
		LOG.info("param1:{}", title);
		LOG.info("param2:{}", desc);
		

		try {
			Resource providerResource = resolver.getResource(providerPath);
			Node providerNode;
			if (providerResource != null) {
				providerNode = providerResource.adaptTo(Node.class);
				if (providerNode.hasNode(JCR_CONTENT)) {
					saveProperties(title, desc, providerList, cssStyles, jsStyles, providerNode,session);
				} else {
					providerNode.addNode(JCR_CONTENT);
					saveProperties(title, desc, providerList, cssStyles, jsStyles, providerNode,session);
				}
				providerNode.getSession().save();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Style Builder Properties", ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while saving style builder properties : " + e);
			res.send(response, false);
		}

	}

	private void saveProperties(String title, String desc, String[] providerList, String[] cssStyles, String[] jsStyles,
			Node providerNode, Session session) throws PathNotFoundException, RepositoryException, ValueFormatException,
			VersionException, LockException, ConstraintViolationException, ItemExistsException {
		Node jcrNode;
		Node styleGroupNode;
		jcrNode = providerNode.getNode(JCR_CONTENT);
		jcrNode.setProperty(JCR_TITLE, title);
		jcrNode.setProperty(JCR_DESCRIPTION, desc);
		jcrNode.setProperty(CORPCC_PROVIDER_LIST, (Value)null);
		jcrNode.setProperty(CORPCC_PROVIDER_LIST, providerList);
		if (jcrNode.hasNode(CQ_STYLE_GROUPS)) {
			jcrNode.getNode(CQ_STYLE_GROUPS).remove();
			jcrNode.addNode(CQ_STYLE_GROUPS);
			session.save();
			styleGroupNode = jcrNode.getNode(CQ_STYLE_GROUPS);
			setGroupStyleProperties(cssStyles, jsStyles, styleGroupNode);
		} else {
			jcrNode.addNode(CQ_STYLE_GROUPS);
			styleGroupNode = jcrNode.getNode(CQ_STYLE_GROUPS);
			setGroupStyleProperties(cssStyles, jsStyles, styleGroupNode);
		}
	}

	private void setGroupStyleProperties(String[] cssStyles, String[] jsStyles, Node styleGroupNode)
			throws RepositoryException, VersionException, LockException, ConstraintViolationException,
			AccessDeniedException, PathNotFoundException, ValueFormatException {
		LOG.info("CQ_CSS_STYLES:{}", Arrays.toString(cssStyles));
		LOG.info("CQ_JS_STYLES:{}", Arrays.toString(jsStyles));
		styleGroupNode.setProperty(CQ_CSS_STYLES, cssStyles);
		styleGroupNode.setProperty(CQ_JS_STYLES, jsStyles);
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