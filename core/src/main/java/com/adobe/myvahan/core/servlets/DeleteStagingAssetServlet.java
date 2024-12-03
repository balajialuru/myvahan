package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMessage;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodExit;
import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.File;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.assets.core.exceptions.LCMSUtilException;
import com.apple.learning.assets.core.services.CorpLearningTaxonomyService;
import com.apple.learning.assets.core.utility.CorpLCMSOSGIUtil;
import com.apple.learning.logging.core.utility.ExceptionCodes;


@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplms/deletestagingasset",
  SLING_SERVLET_METHODS + "=POST" })

public class DeleteStagingAssetServlet extends SlingAllMethodsServlet{

	
	@Reference
	private ResourceResolverFactory resolverFactory;
	/**
	 * 
	 */
	private static final long serialVersionUID = -4332207652412253871L;
	protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
	private static final String CLASS_NAME = DeleteStagingAssetServlet.class.getName();
	private static final String SUCCESS_MESSAGE = "Success";
	private static final String FAILURE_MESSAGE = "Failure";
	private static final int SUCCESS_RESPONSE_CODE = CorpLearningTaxonomyService.PUSHTOLMS_SUCCESS_RESPONSE_CODE;
	private static final int FAILURE_RESPONSE_CODE = CorpLearningTaxonomyService.PUSHTOLMS_FAILURE_RESPONSE_CODE;
	private static final String REPLICATIONPATH_CONFIG = "com.apple.learning.assets.core.replication.CorplmsActivationDirectoryPathSetter";
	private static final String DELETE_ASSET = "Delete Asset";

	
	
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
		LOG.trace(logMethodEntry(DELETE_ASSET, CLASS_NAME, methodName));
		boolean isDeleted = false;
		String assetPath=request.getParameter("assetPath");
		LOG.info(logMessage(DELETE_ASSET, CLASS_NAME, methodName,"Asset to be deleted from staging "+assetPath));
		
		
		CorpLCMSOSGIUtil corpLCMSUtil = newLCMSOsgiUtil();
		try {
			String stagingFilePath = "";
			String stagingNfsPath = corpLCMSUtil.readOsgiConfig(REPLICATIONPATH_CONFIG, "staging.nfs.path");
			LOG.info(logMessage(DELETE_ASSET, CLASS_NAME, methodName,"stagingNfsPath "+stagingNfsPath));
			if(null != assetPath && !assetPath.isEmpty()) {
				stagingFilePath = stagingNfsPath + assetPath.trim();
				LOG.info(logMessage(DELETE_ASSET, CLASS_NAME, methodName,"stagingFilePath "+stagingFilePath));
				isDeleted = deleteAssetFromStaging(stagingFilePath);
			}
		} catch (LCMSUtilException e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_ASSET, ExceptionCodes.Exception,
					"Exception occurred while deleting asset from staging: " + e);		
		}catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_ASSET, ExceptionCodes.Exception,
					"Exception occurred while deleting asset from staging: " + e);		
		}
		if(isDeleted) {
			response.setStatus(SUCCESS_RESPONSE_CODE);
			response.getWriter().write(SUCCESS_MESSAGE);
		}else {
			response.setStatus(FAILURE_RESPONSE_CODE);
			response.getWriter().write(FAILURE_MESSAGE);
		}
		
		LOG.trace(logMethodExit(DELETE_ASSET, CLASS_NAME, methodName));

	}
	
	
	/** 
	 * @param stagingFilePath
	 * @return boolean
	 */
	private boolean deleteAssetFromStaging(String stagingFilePath) {
		String methodName = "deleteAssetFromStaging";
		LOG.trace(logMethodEntry(DELETE_ASSET, CLASS_NAME, methodName));
		boolean isDeleted = false;

		try {
			File file = new File(stagingFilePath);
			if (null != file) {
				if (file.exists()) {
					LOG.debug(logMessage(DELETE_ASSET, CLASS_NAME, methodName,"File deleted from path: " + file.getAbsolutePath()));
					isDeleted = file.delete();
				}else {
					LOG.info(logMessage(DELETE_ASSET, CLASS_NAME, methodName,"File does not exist in staging "));
				}
			}
		}catch (Exception e) {
				logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_ASSET, ExceptionCodes.Exception,
						"Exception occurred while deleting asset from staging: " + e);		
		}	
		LOG.trace(logMethodExit(DELETE_ASSET, CLASS_NAME, methodName));
		return isDeleted;

	}
		

	
	/** 
	 * @return CorpLCMSOSGIUtil
	 */
	public CorpLCMSOSGIUtil newLCMSOsgiUtil() {
		return new CorpLCMSOSGIUtil();
	}
}
