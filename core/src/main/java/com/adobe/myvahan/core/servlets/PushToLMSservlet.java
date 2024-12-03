package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkConstants.PUBLISHING;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.assets.core.exceptions.LCMSUtilException;
import com.apple.learning.assets.core.utility.CorpLCMSOSGIUtil;
import com.apple.learning.corplcms.assetshare.util.CorpLCMUtil;
import com.apple.learning.corplcms.constants.Constants;

/**
 * @author khasimsaheb_shaik 
 *
 */

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/push",
		SLING_SERVLET_METHODS + "=POST" })
public class PushToLMSservlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(PushToLMSservlet.class);
	private static final String CLASS_NAME = PushToLMSservlet.class.getName();
	final String INPUTDATE_FORMAT = "yyyy-MM-dd";
	final String OUTPUTDATE_FORMAT = "MM/dd/yyyy";
	private static final String REPLICATIONPATH_CONFIG = "com.apple.learning.assets.core.replication.CorplmsActivationDirectoryPathSetter";
	@Reference
	private JobManager jobManager;

	
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {
		String methodName = "doPost";
		LOG.debug(logMethodEntry(PUBLISHING, CLASS_NAME, methodName));
		Session session = request.getResourceResolver().adaptTo(Session.class);
	    response.setContentType("text/plain");
		String publisher = session.getUserID();
		String contentType = request.getParameter("contentType");
		String callServiceAPI = request.getParameter("callServiceAPI");
		String assetPath = request.getParameter("selectedPath");
		String locales = request.getParameter("locales");
		String versionStatus = request.getParameter("status");
		String subFolder = request.getParameter("subFolder");
		String fromCard = request.getParameter("fromCard");
		
		String requestProtocol = request.getScheme();
		String coursePathPrefix = requestProtocol + "://" + request.getServerName() + ":"
				+ request.getServerPort();
		logRequestParameter(request,publisher,coursePathPrefix);
		Map<String, Object> pushToLMSJobProps = new HashMap<String, Object>();
		pushToLMSJobProps.put("contentType", contentType);
		pushToLMSJobProps.put("callServiceAPI", callServiceAPI);
		pushToLMSJobProps.put("assetPath", assetPath);
		pushToLMSJobProps.put("locales", locales);
		pushToLMSJobProps.put("versionStatus", versionStatus);
		pushToLMSJobProps.put("publisher",  publisher);
		pushToLMSJobProps.put("coursePathPrefix",  coursePathPrefix);
		LOG.debug("subFolder"+subFolder);
		if (StringUtils.isNoneBlank(subFolder)) {
			LOG.info("assetPath subfolder"+assetPath);
			LOG.info("CorpLCMUtil.getCourseName(assetPath))"+CorpLCMUtil.getCourseName(assetPath));
			if(assetPath.endsWith(CorpLCMUtil.getCourseName(assetPath)))
			{
				pushToLMSJobProps.put("subFolder", Constants.FALSE);
			}
			else
			{
				pushToLMSJobProps.put("subFolder", subFolder);
			}
			
		}
		LOG.debug("fromCard"+fromCard);
		if (StringUtils.isNoneBlank(fromCard)) {
			pushToLMSJobProps.put("fromCard", fromCard);
		}
		jobManager.addJob(getJobName() , pushToLMSJobProps);
	
	}
	
	/** 
	 * @return String
	 */
	private String getJobName()
	{
		String pushToLMSJobName = Constants.PUSHTOLMS_JOB_TOPIC;
		CorpLCMSOSGIUtil corpLCMSUtil = new CorpLCMSOSGIUtil();
		try {
			 pushToLMSJobName = corpLCMSUtil.readOsgiConfig(REPLICATIONPATH_CONFIG, "push.lms.jobname");
		} catch (LCMSUtilException e) {
			LOG.error("Exception on getting jobname from configuration"+e.getMessage());
		}
		LOG.info("pushToLMSJobName"+pushToLMSJobName);
		return pushToLMSJobName;
	}

	
	/** 
	 * @param request
	 * @param publisher
	 * @param coursePathPrefix
	 */
	private void logRequestParameter(final SlingHttpServletRequest request,String publisher,String coursePathPrefix)
	{
		LOG.debug("contentType"+request.getParameter("contentType"));
		LOG.debug("callServiceAPI"+request.getParameter("callServiceAPI"));
		LOG.debug("selectedPath"+request.getParameter("selectedPath"));
		LOG.debug("locales"+ request.getParameter("locales"));
		LOG.debug("status"+ request.getParameter("status"));
		LOG.debug("subFolder"+ request.getParameter("subFolder"));
		LOG.debug("fromCard"+request.getParameter("fromCard"));
		LOG.debug("publisher"+publisher);
		LOG.debug("coursePathPrefix"+coursePathPrefix);
		
	}
}