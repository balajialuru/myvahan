package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.Rendition;
import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.models.ContentType;
import com.apple.learning.corplcms.core.models.DepartmentData;
import com.apple.learning.corplcms.core.models.MultiLMSModel;
import com.apple.learning.corplcms.core.models.ProviderData;
import com.apple.learning.corplcms.core.models.SecureContentData;
import com.apple.learning.corplcms.core.services.impl.ProviderUtil;
import com.apple.learning.logging.core.utility.ExceptionCodes;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * The Class ProviderActionRequestHelper.
 */
public class ProviderActionRequestHelper {
	/** The Constant CLASS_NAME. */
	private static final String CLASS_NAME = ProviderActionRequestHelper.class.getName();

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ProviderActionRequestHelper.class);

	public String SLASH = Constants.SLASH;
	/**
	 * Populate provider data.
	 * @param request the request
	 * @return the provider data
	 */
	public ProviderData populateProviderData(final SlingHttpServletRequest request) {
		final ProviderData providerInfo = new ProviderData();
		providerInfo.setTitle(request.getParameter(Constants.PROVIDERTITLE).trim());
		providerInfo.setName(request.getParameter(Constants.PROVIDERNAME));
		providerInfo.setTemplate(request.getParameter(Constants.TEMPLATE));
		String parentPath = request.getParameter(Constants.PARENTPATH);
		if (StringUtils.isBlank(parentPath)) {
			parentPath = Constants.CONTENTPARENTPATH;
		}
		LOG.info("parentPath" + parentPath);
		providerInfo.setPath(parentPath);
		providerInfo.setOperation(request.getParameter(Constants.OPERATION));
		providerInfo.setActionName(request.getParameter(Constants.BASICACTIONNAME));
		providerInfo.setLanguages(
				ProviderUtil.excludeGlobalLanguage(request.getParameterValues(Constants.LANGUAGES)));
		providerInfo.setSearchFilters(request.getParameterValues(Constants.SEARCHFILTERS));
		providerInfo.setProviderAdminGroup(request.getParameterValues(Constants.PROVIDERADMINGROUP));
		providerInfo.setContentDevGroup(request.getParameterValues(Constants.CONTENTDEVGROUP));
		providerInfo.setProviderId(request.getParameter(Constants.PROVIDERID));
		providerInfo.setCourseContainerTemplates(request.getParameterValues(Constants.COURSE_CONTAINER_TEMPLATE)==null? new String[] {} : request.getParameterValues(Constants.COURSE_CONTAINER_TEMPLATE));
		providerInfo.setCourseTemplates(request.getParameterValues(Constants.COURSE_TEMPLATTES)==null? new String[] {} : request.getParameterValues(Constants.COURSE_TEMPLATTES));
		providerInfo.setProviderTheme(request.getParameter(Constants.PROVIDER_THEME));
		providerInfo.setCmsTranslationEnabled(request.getParameter(Constants.ENABLED_TRANSLATION)==null? Constants.FALSE : request.getParameter(Constants.ENABLED_TRANSLATION));
		populateDepartmentDetails(request, providerInfo);
		populateThumbnailDetails(request, providerInfo);
		LOG.info("populateSecureContentData called" + parentPath);
//		String providerSecure = request.getParameter(Constants.PROVIDER_SECURE);
		String enableCompliance=request.getParameter(Constants.PROVIDER_ENABLE_COMPLIANCE);
		if(enableCompliance == null) {
			enableCompliance = getIfPropExists(request, providerInfo, Constants.PROVIDER_ENABLE_COMPLIANCE);
		}
		providerInfo.setEnableCompliance(enableCompliance);
		
		SecureContentData secureData=populateSecureContentData(request, providerInfo);
		if(secureData!=null && secureData.getSecure()!=null && Constants.TRUE.equals(secureData.getSecure())) {
			providerInfo.setProviderType(Constants.BOTH);
		} else {
			providerInfo.setProviderType(Constants.INTERNAL);
		}
		
		if(secureData!=null) {
			LOG.info("populateSecureContentData set secureData" + secureData);
			providerInfo.setSecureContentData(secureData);
			if (Constants.SECUREENABLE.equals(secureData.getOperation())) {
				providerInfo.setProviderType(Constants.SECURED);
			}
		}
		providerInfo.setMultiLMSModel(getMultiLMSDetails(request, providerInfo));
		
		return providerInfo;
	}

	/**
	 * Populate department details.
	 * @param request the request
	 * @param providerInfo the provider info
	 */
	private void populateDepartmentDetails(final SlingHttpServletRequest request, final ProviderData providerInfo) {
	
		String replaceString;
		String replaceCTString ;
		boolean departMentExist = true;
		String requestDeparTitle;
		String reqDeptName;
		DepartmentData departmentData;
		ContentType contentType;
		List<ContentType> contentTypes;
		final List<DepartmentData> departments = new ArrayList<DepartmentData>();
		int deptTitleIndex = 0;
		int ctTitleIndex = 0;
		while (departMentExist) {
			replaceString = Constants.EMPTYSTRING + deptTitleIndex;
			requestDeparTitle = Constants.DEPARTMENT_TITLE.replace(Constants.X, replaceString);
			reqDeptName = Constants.DEPARTMENT_NAME.replace(Constants.X, replaceString);
			if (request.getParameter(requestDeparTitle) != null) {
				departmentData = new DepartmentData();
				departmentData.setTitle(request.getParameter(requestDeparTitle));
				departmentData.setName(request.getParameter(reqDeptName));
				departmentData.setItemId(Constants.ITEM + deptTitleIndex);
				boolean contentTypeExist = true;
				String requestCTTitle, requestCTName = null;
				contentTypes = new ArrayList<ContentType>();
				ctTitleIndex = 0;
				while (contentTypeExist) {
					replaceCTString = Constants.EMPTYSTRING + ctTitleIndex;
					requestCTTitle = Constants.CONTENTTYPE_TITLE.replace(Constants.X, replaceString)
							.replace(Constants.Y, replaceCTString);
					requestCTName = Constants.CONTENTTYPE_NAME.replace(Constants.X, replaceString).replace(Constants.Y,
							replaceCTString);
					if (request.getParameter(requestCTTitle) != null) {
						contentType = new ContentType();
						contentType.setTitle(request.getParameter(requestCTTitle));
						contentType.setName(request.getParameter(requestCTName));
						contentType.setItemId(Constants.ITEM + ctTitleIndex);

						contentTypes.add(contentType);
					} else {
						contentTypeExist = false;
					}
					ctTitleIndex++;

				}
				departmentData.setContentTypes(contentTypes);
				departments.add(departmentData);
			} else {
				departMentExist = false;
			}
			deptTitleIndex++;
		}
		providerInfo.setDepartments(departments);

	}

	/**
	 * Populate thumbnail details.
	 * @param request the request
	 * @param providerInfo the provider info
	 */
	private void populateThumbnailDetails(final SlingHttpServletRequest request, final ProviderData providerInfo) {
		String methodName = "populateThumbnailDetails";
		final RequestParameter coverImageParam = request.getRequestParameter(Constants.COVERIMAGE);
		String mimeType = null;
		try {
			InputStream thumbnailStream = null;
			if (coverImageParam != null && coverImageParam.getSize() > 0L) {
				mimeType = coverImageParam.getContentType();
				thumbnailStream = coverImageParam.getInputStream();
			} else if (StringUtils.isNotBlank(request.getParameter(Constants.COVERIMAGEASSETPATH))) {
				Rendition originalRendition = ProviderUtil.getOriginalAssetRendition(
						request.getParameter(Constants.COVERIMAGEASSETPATH), request.getResourceResolver());
				if (originalRendition != null) {
					mimeType = originalRendition.getMimeType();
					thumbnailStream = originalRendition.getStream();
				}
			}
			providerInfo.setThumbnailStream(thumbnailStream);
			providerInfo.setMimeType(mimeType);
		} catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Thumbnail Population error on create/update provider",
					ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while populating thumbnail from the request : " + e);
		}
	}
	
	private SecureContentData populateSecureContentData(final SlingHttpServletRequest request, ProviderData providerInfo) {
		LOG.info("populateSecureContentData");
		String parentPath = request.getParameter(Constants.SECUREPARENTPATH);
		if (StringUtils.isBlank(parentPath)) {
			parentPath = Constants.SECURECONTENTPARENTPATH;
		}
		SecureContentData secureContentData = null;
		String providerSecure=request.getParameter(Constants.PROVIDER_SECURE);
		LOG.info("providerSecure" + providerSecure);
		
		if(providerSecure == null ) {
			providerSecure = getIfPropExists(request, providerInfo, Constants.PROVIDER_SECURE);
		}
		LOG.info("providerSecure from internal Provider is" + providerSecure);
		if(Constants.TRUE.equals(providerSecure)) {
			secureContentData = new SecureContentData();
			LOG.info("parentPath" + parentPath);
			secureContentData.setPath(parentPath);
			secureContentData.setSecure(providerSecure);
			secureContentData.setLanguages(
					ProviderUtil.excludeGlobalLanguage(request.getParameterValues(Constants.SECURELANGUAGES)));
			secureContentData.setSearchFilters(request.getParameterValues(Constants.SECURESEARCHFILTERS));
			secureContentData.setProviderAdminGroup(request.getParameterValues(Constants.SECUREPROVIDERADMINGROUP));
			secureContentData.setContentDevGroup(request.getParameterValues(Constants.SECURECONTENTDEVGROUP));
			secureContentData.setOperation(request.getParameter(Constants.SECUREOPERATION));
		}
		return secureContentData;
	}
	
	private String getIfPropExists(final SlingHttpServletRequest request, ProviderData providerInfo, String propName) {
		String methodName= "getIfSecure";
		String providerSecure = "false";
		try {
			String internalParentPath = providerInfo.getPath();
			String providerID = providerInfo.getProviderId();
			if(providerID!=null) {
				String internalProviderPath = internalParentPath+ SLASH + providerID + SLASH + providerID + SLASH + JcrConstants.JCR_CONTENT;
				Session session = request.getResourceResolver().adaptTo(Session.class);
				if(session.itemExists(internalProviderPath)) {
					Node internalProviderNode = session.getNode(internalProviderPath);
					if(internalProviderNode.hasProperty(propName) && internalProviderNode.getProperty(propName)!=null) {
						boolean val = internalProviderNode.getProperty(propName).getBoolean();
						providerSecure = Boolean.toString(val);
					}
				}
			}
		} catch (RepositoryException e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Repo Exception while fetching the provider ",
					ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while fetching the provider : " + e);
			e.printStackTrace();
		}
		return providerSecure;
	}
	
	private String getIfSecure2(final SlingHttpServletRequest request, ProviderData providerInfo) {
		String methodName= "getIfSecure";
		String providerSecure = null;
		try {
			String internalParentPath = providerInfo.getPath();
			String providerID = providerInfo.getProviderId();
			if(providerID!=null) {
				String internalProviderPath = internalParentPath+ SLASH + providerID + SLASH + providerID + SLASH + JcrConstants.JCR_CONTENT;
				Session session = request.getResourceResolver().adaptTo(Session.class);
				if(session.itemExists(internalProviderPath)) {
					Node internalProviderNode = session.getNode(internalProviderPath);
					if(internalProviderNode.hasProperty(Constants.PROVIDER_SECURE) && internalProviderNode.getProperty(Constants.PROVIDER_SECURE)!=null) {
						boolean val = internalProviderNode.getProperty(Constants.PROVIDER_SECURE).getBoolean();
						providerSecure = Boolean.toString(val);
					}
				}
			}
		} catch (RepositoryException e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Repo Exception while fetching the provider ",
					ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while fetching the provider : " + e);
			e.printStackTrace();
		}
		return providerSecure;
	}
	
	private MultiLMSModel getMultiLMSDetails(final SlingHttpServletRequest request, ProviderData providerInfo) {
		MultiLMSModel multiLMSModel = new MultiLMSModel();
		boolean multiPushforCC =  (request.getParameter(Constants.CORP_MUTLI_PUSH_CC) == null ) ? false : Boolean.parseBoolean(request.getParameter(Constants.CORP_MUTLI_PUSH_CC)) ;
		multiLMSModel.setMultiPushEnabledCC(multiPushforCC); 
		multiLMSModel.setOtherLMSForCC(request.getParameterValues(Constants.OTHER_LMS_CC));
		multiLMSModel.setLmsDefaultCC(request.getParameter(Constants.OTHER_LMS_DEFAULT_CC));
		
		boolean multiPushforBYOC =  (request.getParameter(Constants.CORP_MUTLI_PUSH_BYOC) == null ) ? false : Boolean.parseBoolean(request.getParameter(Constants.CORP_MUTLI_PUSH_BYOC)) ;
		multiLMSModel.setMultiPushEnabledBYOC(multiPushforBYOC); 
		multiLMSModel.setOtherLMSForBYOC(request.getParameterValues(Constants.OTHER_LMS_BYOC));
		multiLMSModel.setLmsDefaultBYOC(request.getParameter(Constants.OTHER_LMS_DEFAULT_BYOC));
		return multiLMSModel;
	}
}
