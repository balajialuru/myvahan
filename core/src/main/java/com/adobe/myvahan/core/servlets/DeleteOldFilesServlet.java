package com.apple.learning.corplcms.core.servlets;


import static com.apple.learning.logging.core.splunk.SplunkConstants.DELETE_OLD_FILES;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodExit;
import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.assets.core.services.CommonUtilService;
import com.apple.learning.assets.core.services.CorpLearningTaxonomyService;
import com.apple.learning.corplcms.core.services.DeleteOldFilesService;
import com.apple.learning.corplcms.core.threads.DeleteOldFilesThread;
import com.apple.learning.logging.core.utility.ExceptionCodes;

@Component(service=Servlet.class, property = {"sling.servlet.paths=/bin/corplcms/deleteNonDuplicateUploads"})
@Designate(ocd = DeleteOldFilesServlet.DeleteOldFilesServletConf.class)
public class DeleteOldFilesServlet extends SlingAllMethodsServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1749062807173311869L;

	private static final String CLASS_NAME = DeleteOldFilesServlet.class.getName();
	
	private static final Logger LOG = LoggerFactory.getLogger(DeleteOldFilesServlet.class);
	
	@Reference
	ResourceResolverFactory resourceResolverFactory;
	
	@Reference
	DeleteOldFilesService deleteOldFilesService;
	
	@Reference
	private CommonUtilService commonUtilService;
	
	private ExecutorService executor;
	
	private static final boolean ACTIVATE_THREADING_DEFAULT = true;	
	private boolean activateThread = ACTIVATE_THREADING_DEFAULT;
	private int threadPoolSize;
	
	@ObjectClassDefinition(name="Corplcms Delete Old Files Servlet")
    public @interface DeleteOldFilesServletConf {
        @AttributeDefinition(name = "threadPoolSize",description = "Thread pool size")
        int threadPoolSize() default 2;
        
        @AttributeDefinition(name = "Activate Thread to use threadppol for Delete Old Files.",description = "Check to activate threadpool")
        boolean activate_thread_activator() default ACTIVATE_THREADING_DEFAULT;
    }
	
	
	/** 
	 * @param config
	 */
	@Activate
	protected final void activate(DeleteOldFilesServletConf config) {
		String methodName = "activate";
		this.activateThread = config.activate_thread_activator();
		this.threadPoolSize = config.threadPoolSize();
		if(this.activateThread) {
			try {
				this.executor = Executors.newFixedThreadPool(this.threadPoolSize);
			} catch(Exception e) {
				logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.Exception , "Exception occurred: IllegalArgumentExcption while creating ThreadPool - illegal value for threadpoolsize "+this.threadPoolSize+ ". Using defalut thread pool size - 2");
				this.executor = Executors.newFixedThreadPool(2);
			}
		}
	}
	
	
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
		String methodName = "doPost";
		LOG.trace(logMethodEntry(DELETE_OLD_FILES, CLASS_NAME, methodName));
		
		String folderUpload = request.getParameter("folderUpload");
		String contentPath = request.getParameter("contentPath");
		String localeJSON = request.getParameter("localeJSON");
		String[] uploadedFolders = request.getParameterValues("deleteParentpaths");
		LOG.trace("uploadedFolders : "+Arrays.toString(uploadedFolders));
		String[] uploadedFiles= request.getParameterValues("currentFileList");
		LOG.trace("uploadedFiles : "+Arrays.toString(uploadedFiles));
		String[] localesUpdated = request.getParameterValues("localesUpdated");
		LOG.debug("localesUpdated : "+Arrays.toString(localesUpdated));
		
		ResourceResolver resourceResolver = null;
		Session session = null;
		ResourceResolver userresolver = null;
		String userID = null;
		try {
			userresolver = request.getResourceResolver();
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);
			userID = userresolver.getUserID();
			JSONObject json = new JSONObject(localeJSON);
			if(this.activateThread) {
				this.executor.submit(new DeleteOldFilesThread(deleteOldFilesService, commonUtilService, uploadedFolders, uploadedFiles, localesUpdated, contentPath, userID, resourceResolver, json, folderUpload.equals("true")));
			} else {
				session = resourceResolver.adaptTo(Session.class);
				Workspace workspace = session.getWorkspace();				
				if(folderUpload.equals("true")) {
					deleteOldFilesService.deleteExistingAssets(uploadedFolders,uploadedFiles,localesUpdated, workspace, session, CorpLearningTaxonomyService.OPERATION_UPLOAD);
					deleteOldFilesService.deleteEmptyFolders(uploadedFolders,json,  workspace, session);
				}
			}
			response.setStatus(200);
		}catch (JSONException e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.JSONException , "Exception occurred: JSONException"+e);
		} catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.Exception , "Exception occurred: "+e);
		} finally {
			if(!this.activateThread) {
				commonUtilService.closeResolverSession(session, resourceResolver);
			}
		} 
		LOG.trace(logMethodExit(DELETE_OLD_FILES, CLASS_NAME, methodName));
	}
	
	
	/** 
	 * @param resourceResolverFactory
	 * @param deleteOldFilesService
	 * @param commonUtilService
	 */
	public void setForTestability(ResourceResolverFactory resourceResolverFactory,DeleteOldFilesService deleteOldFilesService, CommonUtilService commonUtilService) {
		this.resourceResolverFactory = resourceResolverFactory;
		this.deleteOldFilesService = deleteOldFilesService;
		this.commonUtilService = commonUtilService;
	}
}
