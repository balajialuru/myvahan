package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.course.container.services.api.ConsumerConfigService;
import com.apple.learning.logging.core.splunk.SplunkFormatter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplms/grovePreview",
		SLING_SERVLET_METHODS + "=GET" })
public class GrovePreviewDetailsServlet extends SlingAllMethodsServlet {

	private final static Logger LOG = LoggerFactory.getLogger(GrovePreviewDetailsServlet.class);
	private final static String CLASS_NAME = GrovePreviewDetailsServlet.class.getName();
	private static final String GROVE_PREVIEW_DETAILS_SERVLET = "GrovePreviewDetailsServlet";
	private static final String COURSE_STRUCTURE = "course-structure";
	private static final String CHAPTERS = "chapters";
	private static final String JCR_CONTENT = "jcr:content";
	private static final String JCR_TITLE = "jcr:title";
	private static final String UTF_8 = "UTF-8";
	private static final String HTML = ".html";
	private static final String PAGE_NAME = "pageName=";
	private static final String CHAPTER_NAME = "chapterName=";
	private static final String LOCALE = "locale=";
	private static final String TITLE = "title=";
	private static final String EMPERSAND = "&";
	private static final String EN_GLOBAL = "en_Global";
	private static final String SLASH = "/";
	private static final String CHAPTER1 = "Chapter1";
	private static final String DOT_PREVIEW_HTML =".preview.html";
	private static final String DOT_HTML =".html";
	private static final long serialVersionUID = 12L;
	private static final String PAGES = "pages";
	private static final String PAGE_PATH = "corpcc:pagePath";
	private static final String CORPCC_CHAPTER = "corpcc:chapter";

	@Reference
	private SlingSettingsService slingSettingsService;

	@Reference
	private ResourceResolverFactory resolverFactory;

	@Reference
	private ConsumerConfigService consumerConfigService;

	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		JsonObject json = new JsonObject();
		String methodName = "doGet";
		String previewPage = request.getParameter("previewPage");
		try {
			LOG.info("Inside getRunModes servlet:{}", slingSettingsService.getRunModes());
			String page = previewPage.split(HTML)[0] + SLASH + JCR_CONTENT;
			String course = previewPage.substring(0, previewPage.lastIndexOf(SLASH) + 1) + JCR_CONTENT;
			ResourceResolver resourceResolver = resolverFactory.getServiceResourceResolver(null);
			Session session = resourceResolver.adaptTo(Session.class);
			Node pageNode = session.getNode(page);
			Node courseNode = session.getNode(course);
			String pageTitle = pageNode.getProperty(JCR_TITLE).getString();
			String courseTitle = courseNode.getProperty(JCR_TITLE).getString();
			String chapterTitle = CHAPTER1;
			String domain = "thegrove://contentpreview?url=";
			LOG.info("pageTitle:{}", pageTitle);
			LOG.info("courseTitle:{}", courseTitle);

			String vipUrl = consumerConfigService.getVipURL();
			if (vipUrl.isEmpty()) {
				vipUrl = "http://localhost:4502";
			}

			chapterTitle = getChapterTitle(courseNode, previewPage.split(HTML)[0], chapterTitle);
			String[] pathList = previewPage.split(SLASH);
			String locale = pathList[5].equalsIgnoreCase(EN_GLOBAL) ? pathList[5].toUpperCase()
					: pathList[6].toUpperCase();
			String courseParameter = EMPERSAND + TITLE + courseTitle + EMPERSAND + LOCALE + locale + EMPERSAND
					+ CHAPTER_NAME + chapterTitle + EMPERSAND + PAGE_NAME + pageTitle;
			String previewUrl = previewPage.split(DOT_HTML)[0];
			previewUrl=previewUrl+DOT_PREVIEW_HTML;
			String pageUrl = vipUrl + previewUrl;
			URL url = new URL(pageUrl + courseParameter);
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), IDN.toASCII(url.getHost()), url.getPort(),
					url.getPath(), url.getQuery(), url.getRef());
			String encodedPageUrl = uri.toASCIIString();
			String previewPageUrl = domain + encodedPageUrl;
			if(previewPage.startsWith(Constants.SECURE_PROVIDER_PATH)) {
				previewPageUrl = previewPageUrl.concat(Constants.SECURE_SUFFIX);
			}
			LOG.info("previewPageUrl:{}", previewPageUrl);

			json.addProperty("previewPageUrl", previewPageUrl);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String prettyJson = gson.toJson(json);
			response.setContentType("application/json");
			response.setCharacterEncoding(UTF_8);
			response.getWriter().write(prettyJson);
		} catch (PathNotFoundException e) {
			LOG.error(SplunkFormatter.logException(GROVE_PREVIEW_DETAILS_SERVLET, CLASS_NAME, methodName,
					"PathNotFoundException occurred:" + e.getMessage()), e);
		} catch (RepositoryException e) {
			LOG.error(SplunkFormatter.logException(GROVE_PREVIEW_DETAILS_SERVLET, CLASS_NAME, methodName,
					"RepositoryException occurred:" + e.getMessage()), e);
		} catch (LoginException e) {
			LOG.error(SplunkFormatter.logException(GROVE_PREVIEW_DETAILS_SERVLET, CLASS_NAME, methodName,
					"LoginException occurred:" + e.getMessage()), e);
		} catch (URISyntaxException e) {
			LOG.error(SplunkFormatter.logException(GROVE_PREVIEW_DETAILS_SERVLET, CLASS_NAME, methodName,
					"URISyntaxException occurred:" + e.getMessage()), e);
		}
	}

	private String getChapterTitle(Node courseNode, String pagePath, String chapterTitle) throws RepositoryException {
		String chapter = null;
		if (courseNode.hasNode(COURSE_STRUCTURE)) {
			Node structNode = courseNode.getNode(COURSE_STRUCTURE);
			Node pages = structNode.getNode(PAGES);
			NodeIterator pagesList = pages.getNodes();
			while(pagesList.hasNext()) {
				Node page = pagesList.nextNode();
				if(page.hasProperty(PAGE_PATH) && page.getProperty(PAGE_PATH)!=null && page.hasProperty(CORPCC_CHAPTER) && page.getProperty(CORPCC_CHAPTER)!=null) {
					String pagePathFromNode = page.getProperty(PAGE_PATH).getString();
					if(pagePath.equals(pagePathFromNode)) {
						chapter = page.getProperty(CORPCC_CHAPTER).getString();
						Node chapterNode = structNode.getNode(CHAPTERS).getNode(chapter);
						if(chapterNode!=null && chapterNode.hasProperty(JCR_TITLE) && chapterNode.getProperty(JCR_TITLE)!=null) {
							chapterTitle = chapterNode.getProperty(JCR_TITLE).getString();
						}
					}
				}
			}
		}
		LOG.info(" The chapter Title is " + chapterTitle);
		return chapterTitle;
	}
}
