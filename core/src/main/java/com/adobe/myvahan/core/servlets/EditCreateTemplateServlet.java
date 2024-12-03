package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodExit;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.granite.ui.components.HtmlResponse;
import com.adobe.granite.xss.XSSAPI;
import com.apple.learning.corplcms.assetshare.util.QueryBuilderUtil;
import com.apple.learning.corplcms.constants.Constants;
import com.day.cq.i18n.I18n;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.TemplateManager;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/templaatecreation",
		SLING_SERVLET_METHODS + "=POST" })
public class EditCreateTemplateServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private final static Logger LOG = LoggerFactory.getLogger(EditCreateTemplateServlet.class);

	public static final String CLASS_NAME = EditCreateTemplateServlet.class.getName();

	private Session userSession;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Reference
	private QueryBuilder queryBuilder;

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		String methodName = "doPost";
		LOG.trace(logMethodEntry("Entering do post", CLASS_NAME, methodName));
		I18n i18n = new I18n(request);
		XSSAPI xssAPI = request.getResourceResolver().adaptTo(XSSAPI.class);
		HtmlResponse res = new HtmlResponse(xssAPI, i18n, request.getLocale());
		ResourceResolver resourceResolver = request.getResourceResolver();
		TemplateManager templateManager = (TemplateManager) resourceResolver.adaptTo(TemplateManager.class);
		Session session = (Session) resourceResolver.adaptTo(Session.class);
		try {
			String type = request.getParameter("type");
			if (!"enable".equalsIgnoreCase(type)) {
				String parentPath = Constants.TEMPLATE_ROOTPATH;
				Resource parent = getParentResource(resourceResolver, parentPath);
				if (StringUtils.isEmpty(parentPath) || ("".equals(parentPath) && parent == null)) {
					this.LOG.error("Parent path can't be empty or is invalid.");
					res.setStatus(503, "Parent path can't be empty or is invalid.");
					res.send((HttpServletResponse) response, true);
					return;
				}
				String templateType = Constants.TEMPLATETYPE_PATH;
				String templateName = request.getParameter(Constants.JCR_TITLE_DOT);
				String description = request.getParameter(Constants.JCR_DESCRIPTION_DOT);
				String hidden = request.getParameter(Constants.HIDDEN_DOT);
				String status = request.getParameter(Constants.STATUS_DOT);
				String providerList = request.getParameter(Constants.PROVIDER_LIST_DOT);
				if (templateManager != null) {
					ValueMapDecorator valueMapDecorator = new ValueMapDecorator(new HashMap<>());
					valueMapDecorator.put(Constants.JCR_DESCRIPTION, description);
					valueMapDecorator.put(Constants.JCRTITLE, templateName);
					valueMapDecorator.put(Constants.HIDDEN, hidden);
					valueMapDecorator.put(Constants.STATUS, status);
					Template template = templateManager.createTemplate(parent.getPath(), templateType, templateName,
							(ValueMap) valueMapDecorator);
					Node templateNode = session.getNode(template.getPath());
					Node jcrContent = templateNode.getNode(Constants.JCRCONTENT);
					jcrContent.addMixin(Constants.MIX_VERSIONABLE);
					jcrContent.setProperty(Constants.PROVIDER_LIST, providerList);
					jcrContent.setProperty(Constants.TEMPLATE_TYPE, Constants.COURSE);
					session.save();
					LOG.info("template:::" + template.getPath());
					res.onCreated(template.getPath());
					res.setStatus(201, i18n.get("Template created"));
					res.setTitle(i18n.get("Template created"));
					res.setPath(template.getPath());
					res.setLocation(template.getPath());
					res.setParentLocation(parent.getPath());
					res.send((HttpServletResponse) response, true);
					LOG.trace(logMethodExit("Exiting do post", CLASS_NAME, methodName));
				}
			} else {
				String templatePaths[] = request.getParameterValues("templatePath");
				if (templatePaths != null) {
					for (String path : templatePaths) {
						Node templateNode = session.getNode(path);
						Node jcrContent = templateNode.getNode(Constants.JCRCONTENT);
						if (jcrContent.hasProperty("corpcc:providerList")) {
							String providerID = jcrContent.getProperty("corpcc:providerList").getString();

							if (providerID != null && !providerID.equalsIgnoreCase(Constants.EDITABLE_ALL)) {
								updateProviders(providerID, path, resourceResolver);

							} else {
								getProviderList(path, resourceResolver);
							}
						}
					}
				}
			}
		} catch (Exception re) {
			try {
				if (session.hasPendingChanges())
					session.refresh(true);
			} catch (RepositoryException rei) {
				LOG.error("Unable to refresh session: ", (Throwable) rei);
			}
			LOG.error("Unable to create template", re);
			res.setStatus(500, "Unable to create template.");
			res.send((HttpServletResponse) response, true);
		}
	}

	private Resource getParentResource(ResourceResolver resourceResolver, String parentPath) {
		Resource parent = resourceResolver.getResource(parentPath);
		Resource templateRoot = parent.getChild(Constants.TEMPLATE_CHILD_NODE);
		boolean hasTemplateStructure = (templateRoot != null);
		if (hasTemplateStructure)
			return templateRoot;
		boolean isOnTemplateRoot = StringUtils.endsWith(parent.getPath(), "settings/wcm/templates");
		if (isOnTemplateRoot)
			return parent;
		return null;
	}

	private void updateProviders(String providerID, String templatePath, ResourceResolver resourceResolver) {
		String methodName = "updateProviders";
		LOG.trace(logMethodEntry("Entering updateProviders", CLASS_NAME, methodName));
		try {
			userSession = resourceResolver.adaptTo(Session.class);
			String providerRootPath = Constants.CONTENTPARENTPATH;
			String providerPath = providerRootPath + Constants.FORWARD_SLASH + providerID + Constants.FORWARD_SLASH
					+ providerID;
			Node providerNode = userSession.getNode(providerPath);
			Node providerContentNode = providerNode.getNode(Constants.JCRCONTENT);
			Resource resource = resourceResolver.getResource(providerContentNode.getPath());
			ValueMap property = resource.adaptTo(ValueMap.class);
			String[] courseTemplates = property.get(Constants.COURSE_TEMPLATTES, String[].class);
			boolean templatePathExist = Arrays.asList(courseTemplates).contains(templatePath);
			if (courseTemplates != null && courseTemplates.length >= 1) {
				String[] templatesNew = new String[courseTemplates.length + 1];
				for (int i = 0; i < courseTemplates.length; i++) {
					templatesNew[i] = courseTemplates[i];
				}
				if(!templatePathExist) {
					templatesNew[courseTemplates.length] = templatePath;
					providerContentNode.setProperty(Constants.COURSE_TEMPLATTES, templatesNew);
				}
			} else {
				providerContentNode.setProperty(Constants.COURSE_TEMPLATTES, new String[] { templatePath });
			}
			userSession.save();
			LOG.trace(logMethodExit("Exiting updateProviders", CLASS_NAME, methodName));
		} catch (RepositoryException e) {
			LOG.error("Unable to updateProviders", e);
		}
	}

	private void getProviderList(String templatePath, ResourceResolver resourceResolver) {
		String methodName = "getProviderList";
		LOG.trace(logMethodEntry("Entering getProviderList", CLASS_NAME, methodName));
		userSession = resourceResolver.adaptTo(Session.class);
		Map<String, String> queryMap = QueryBuilderUtil.fetchProviderList(Constants.CONTENTPARENTPATH);
		Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), userSession);
		SearchResult results = query.getResult();
		if (null != results && null != results.getHits() && results.getHits().size() > 0) {
			Iterator<Resource> assetResources = results.getResources();
			while (assetResources.hasNext()) {
				Resource assetResource = assetResources.next();
				if (assetResource.adaptTo(ValueMap.class).containsKey(Constants.JCRTITLE)) {
					String providerId = assetResource.getParent().getName();
					updateProviders(providerId, templatePath, resourceResolver);
				}
			}
		}
		LOG.trace(logMethodExit("Exiting getProviderList", CLASS_NAME, methodName));

	}

}
