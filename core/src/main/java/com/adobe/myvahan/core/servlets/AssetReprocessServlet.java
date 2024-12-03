package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.assets.core.services.AssetsReplicationService;
import com.apple.learning.corplcms.assetshare.util.CorpLCMUtil;
import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.services.ReProcessService;
import com.apple.learning.logging.core.utility.ExceptionCodes;


@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplcms/reprocessServlet",
		SLING_SERVLET_METHODS + "=POST" })
public class AssetReprocessServlet extends SlingAllMethodsServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final static Logger LOG = LoggerFactory.getLogger(AssetReprocessServlet.class);

	public static final String REPROCESSING = "Asset Reprocessing";

	public static final String CLASS_NAME = AssetReprocessServlet.class.getName();

	@Reference
	private AssetsReplicationService assetReplicationService;

	private List<ReProcessService> reprocessList;

	
	/** 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		String methodName = "doPost";
		ResourceResolver resourceResolver = null;
		Session session = null;

		try {
			resourceResolver = request.getResourceResolver();
			session = resourceResolver.adaptTo(Session.class);

			RequestParameterMap params = request.getRequestParameterMap();
			String path = params.getValue("path").getString();
			String uploadStatus = params.getValue("uploadStatus")!=null?params.getValue("uploadStatus").getString():null;
			String stageStatus = params.getValue("stageStatus")!=null?params.getValue("stageStatus").getString():null;
			String imgRenditonStatus = params.getValue("imgStatus")!=null?params.getValue("imgStatus").getString():null;
			String videoRenditionStatus = params.getValue("videoStatus")!=null?params.getValue("videoStatus").getString():null;
			LOG.info("path" + path);
			LOG.info("uploadStatus" + uploadStatus);
			LOG.info("stageStatus" + stageStatus);
			LOG.info("imgRenditonStatus" + imgRenditonStatus);
			LOG.info("videoRenditionStatus" + videoRenditionStatus);
			if (Constants.FAILED.equals(uploadStatus) || Constants.INVALID.equals(uploadStatus)
					&& Constants.NA.equals(CorpLCMUtil.getReprocessStatus(session, path, Constants.MANIFESTREPROCESS) )){
				LOG.info(Constants.MANIFESTREPROCESS);
				CorpLCMUtil.setReprocessStatus(session, path, Constants.MANIFESTREPROCESS,
						Constants.CAMELCASEPROCESSEING);
				reprocessContent(Constants.MANIFESTREPROCESS, path);	
				CorpLCMUtil.setReprocessStatus(session, path, Constants.MANIFESTREPROCESS, null);
			}
			String stageReprocessStatus = CorpLCMUtil.getReprocessStatus(session, path, Constants.STAGEREPROCESS);
			
			if (!Constants.NA.equals(Constants.NA) && stageReprocessStatus.contains("Invalid_"))
			{
				CorpLCMUtil.setReprocessStatus(session, path, Constants.STAGEREPROCESS, null);
			}
			if (Constants.PRE_STAGE.equals(stageStatus)
					&&( Constants.NA.equals(stageReprocessStatus)
							|| stageReprocessStatus.contains("Invalid_"))) {
				LOG.info(Constants.STAGEREPROCESS);
				CorpLCMUtil.setReprocessStatus(session, path, Constants.STAGEREPROCESS, Constants.CAMELCASEPROCESSEING);
				reprocessContent(Constants.STAGEREPROCESS, path);
//				if(!"InValid Agent Configuration".equals(CorpLCMUtil.getReprocessStatus(session, path, Constants.STAGEREPROCESS)))
//				{
//					CorpLCMUtil.setReprocessStatus(session, path, Constants.STAGEREPROCESS, null);
//				}

			}
			if (Constants.IN_PROCESSEING.equals(imgRenditonStatus)
					&& Constants.NA.equals(CorpLCMUtil.getReprocessStatus(session, path, Constants.IMAGEREPROCESS))) {
				LOG.info(Constants.IMAGEREPROCESS);
				CorpLCMUtil.setReprocessStatus(session, path, Constants.IMAGEREPROCESS, Constants.CAMELCASEPROCESSEING);
				reprocessContent(Constants.IMAGEREPROCESS, path);
				CorpLCMUtil.setReprocessStatus(session, path, Constants.IMAGEREPROCESS, null);
			}
			if (Constants.FAILED.equals(videoRenditionStatus) 
					&& Constants.NA.equals(CorpLCMUtil.getReprocessStatus(session, path, Constants.VIDEOREPROCESS))) {
				LOG.info(Constants.VIDEOREPROCESS);
				CorpLCMUtil.setReprocessStatus(session, path, Constants.VIDEOREPROCESS, Constants.CAMELCASEPROCESSEING);
				reprocessContent(Constants.VIDEOREPROCESS, path);
//				CorpLCMUtil.setReprocessStatus(session, path, Constants.VIDEOREPROCESS, null);
			}
			String assetPath = params.getValue("assetPath")!=null?params.getValue("assetPath").getString():null;
			if (assetPath!=null && ("processAsset".equals(videoRenditionStatus)
					&& Constants.NA.equals(CorpLCMUtil.getReprocessStatus(session, assetPath, Constants.VIDEOREPROCESS)))) {
				LOG.info(Constants.VIDEOREPROCESS);
			
				CorpLCMUtil.setReprocessStatus(session, assetPath, Constants.VIDEOREPROCESS, Constants.CAMELCASEPROCESSEING);
				LOG.info("asset path to reprocess" + assetPath);
				reprocessAssetContent(Constants.VIDEOREPROCESS,assetPath);
//				CorpLCMUtil.setReprocessStatus(session, path, Constants.VIDEOREPROCESS, null);
			}

			
			response.getWriter().write("success");

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace();
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, REPROCESSING, ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while reprocessing" + e);
		}
	}

	
	/** 
	 * @param courseImport
	 */
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void bind(ReProcessService courseImport) {
		if (reprocessList == null) {
			reprocessList = new ArrayList<ReProcessService>();
		}
		reprocessList.add(courseImport);
	}

	
	/** 
	 * @param courseImport
	 */
	protected void unbind(ReProcessService courseImport) {
		reprocessList.remove(courseImport);
	}

	
	/** 
	 * @param name
	 * @param coursePath
	 */
	public void reprocessContent(String name, String coursePath) {
		for (ReProcessService reProcessServiceImpl : reprocessList) {
			if (reProcessServiceImpl.canReProcess(name)) {
				reProcessServiceImpl.doReprocess(coursePath);
				;
				break;
			}
		}
	}

	
	/** 
	 * @param name
	 * @param assetPath
	 */
	public void reprocessAssetContent(String name, String assetPath) {
		for (ReProcessService reProcessServiceImpl : reprocessList) {
			if (reProcessServiceImpl.canReProcess(name)) {
				reProcessServiceImpl.doAssetReprocess(assetPath);
				;
				break;
			}
		}
	}

}