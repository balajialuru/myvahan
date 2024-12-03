package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkConstants.DOWNLOAD_ASSETS;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMessage;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodExit;
import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.net.URLDecoder;
import java.rmi.ServerException;
import java.util.Arrays;
import java.util.zip.ZipOutputStream;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.assets.core.services.CommonUtilService;
import com.apple.learning.assets.core.services.CorpLearningTaxonomyService;
import com.apple.learning.corplcms.core.services.AssetDownloadService;
import com.apple.learning.logging.core.utility.ExceptionCodes;

@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/assetdownload",
  SLING_SERVLET_METHODS +"=GET"})

public class AssetDownloadServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 279793805578840856L;

	protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
	private static final String CLASS_NAME = AssetDownloadServlet.class.getName();
	
	private static final String CHARSET_UTF_8 = "UTF-8"; 
	
	@Reference
	private ResourceResolverFactory resolverFactory;
	
	@Reference
	private AssetDownloadService assetDownloadService;
	
	@Reference
	private CommonUtilService commonUtilService;
	
	
	/** 
	 * @param request
	 * @param response
	 * @throws ServerException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServerException, IOException {
		String methodName = "doGet";
		LOG.trace(logMethodEntry(DOWNLOAD_ASSETS, CLASS_NAME, methodName));
		ResourceResolver resourceResolver = null, userResourceResolver = null;
		Session session = null, userSession = null;
		ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
		String[] paths = null;
		try {
			resourceResolver = resolverFactory.getServiceResourceResolver(null);
			session = resourceResolver.adaptTo(Session.class);
			userResourceResolver = request.getResourceResolver();
			userSession = userResourceResolver.adaptTo(Session.class);
			paths = request.getParameterValues("path");
			String downloadAssets = request.getParameter("downloadAssets");
			downloadAssets = "true";
			String isCollection = request.getParameter("collection");
			String zipFileName = request.getParameter("fileName");
			if(!zipFileName.endsWith(CorpLearningTaxonomyService.ZIP_KEYWORD)) {
				zipFileName = zipFileName+CorpLearningTaxonomyService.ZIP_KEYWORD;
			}
			zipFileName = URLDecoder.decode(zipFileName, CHARSET_UTF_8);
			
			if (paths.length >= 1) {
				response.setContentType(CorpLearningTaxonomyService.APPLICATION_ZIP_KEYWORD);
				response.setHeader("Content-Disposition", "attachment;filename=" + zipFileName);
				assetDownloadService.getZipByteArray(resourceResolver, session, userSession.getUserID(), paths, downloadAssets, zipFileName, out, isCollection);
			}
			LOG.debug(logMessage(DOWNLOAD_ASSETS, CLASS_NAME, methodName, "The ZIP is sent"));
		} catch (Exception e) {
			if(e.toString().contains("ClientAbortException")) {
 				LOG.error(logMessage(DOWNLOAD_ASSETS, CLASS_NAME, methodName, " Exception occured might be because user aborted the download: " + e));
 			} else {
 				logExceptionToSplunk(LOG, CLASS_NAME, methodName, DOWNLOAD_ASSETS, ExceptionCodes.Exception , "Exception occurred: while Downloading Assets at paths : "+(paths != null ? Arrays.toString(paths) : paths)+e);
 			}
		} finally {
			 commonUtilService.closeResolverSession(session, resourceResolver);
			 commonUtilService.closeResolverSession(userSession, userResourceResolver);
			 IOUtils.closeQuietly(out);
		}
		LOG.trace(logMethodExit(DOWNLOAD_ASSETS, CLASS_NAME, methodName));
	}
	
	
	/** 
	 * @param resolverFactory
	 * @param assetDownloadService
	 * @param commonUtilService
	 */
	public void setForTestability(ResourceResolverFactory resolverFactory, AssetDownloadService assetDownloadService, CommonUtilService commonUtilService) {
		this.resolverFactory = resolverFactory;
		this.assetDownloadService = assetDownloadService;
		this.commonUtilService = commonUtilService;
	}
}