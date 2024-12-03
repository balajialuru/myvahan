package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.Rendition;
import com.adobe.granite.ui.components.HtmlResponse;
import com.adobe.granite.xss.XSSAPI;
import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.models.ContentType;
import com.apple.learning.corplcms.core.models.DepartmentData;
import com.apple.learning.corplcms.core.models.ProviderData;
import com.apple.learning.corplcms.core.services.ProviderCreationService;
import com.apple.learning.logging.core.utility.ExceptionCodes;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.PageManagerFactory;

@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/providercreation",
  SLING_SERVLET_METHODS + "=POST" })

public class ProviderCreationServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = -1238148292770358628L;

	private static final String CLASS_NAME = ProviderCreationServlet.class.getName();

	private final static Logger LOG = LoggerFactory.getLogger(ProviderCreationServlet.class);

	@Reference
	ResourceResolverFactory resourceResolverFactory;

	@Reference
	SlingRepository repository;
	
	/** The lms services. */
	private transient List<ProviderCreationService> providerCreationServices;

	@Reference
	protected PageManagerFactory pageManagerFactory;

	
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		LOG.info("In DoGet method");
		doPost(request, response);
	}

	
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
		LOG.debug("In DoPost method");
		I18n i18n = new I18n(request);
		ProviderData providerInfo = new ProviderData();
		XSSAPI xssAPI = request.getResourceResolver().adaptTo(XSSAPI.class);
		HtmlResponse res = new HtmlResponse(xssAPI, i18n, request.getLocale());
		String operation = request.getParameter(Constants.OPERATION);
		String providerType =  request.getParameter(Constants.PROVIDER_TYPE)!=null?request.getParameter(Constants.PROVIDER_TYPE):"CCProvider";
		ResourceResolver resourceResolver = null;
		RequestParameter thumbnailParameter;
		String mimeType = null;
		InputStream thumbnailStream = null;
		resourceResolver = request.getResourceResolver();
		String template = request.getParameter(Constants.TEMPLATE);
		String parentPath = request.getParameter(Constants.PARENTPATH);
		String title = request.getParameter(Constants.PROVIDERTITLE);
		String providerAdminGroup[] = request.getParameterValues(Constants.PROVIDERADMINGROUP);
		String contentDevGroup[] = request.getParameterValues(Constants.CONTENTDEVGROUP);
		String name = request.getParameter(Constants.PROVIDERNAME);
		String searchFilters[] = request.getParameterValues(Constants.SEARCHFILTERS);
		String languages[] = request.getParameterValues(Constants.LANGUAGES);
		String providerId = request.getParameter(Constants.PROVIDERID);
		String ccTemplates[] = request.getParameterValues(Constants.COURSE_CONTAINER_TEMPLATE);
		String courseTemplates[] = request.getParameterValues(Constants.COURSE_TEMPLATTES);
		String providerSecure=request.getParameter(Constants.PROVIDER_SECURE);
		
		List<String> list = new ArrayList<>(Arrays.asList(languages));
		for (String language : languages) {
			
			if(language != null && language.contains("Global")){
				list.remove(language);
				
			}
			
		}
		String languagesNew[]=list.toArray((new String[0]));

		if (StringUtils.isBlank(parentPath)) {
			parentPath = Constants.CONTENTPARENTPATH;
		}
		parentPath="/content/corplcms/providers";
		providerInfo.setTitle(title);
		providerInfo.setName(name);
		providerInfo.setTemplate(template);
		providerInfo.setPath(parentPath);
		providerInfo.setOperation(operation);
		providerInfo.setLanguages(languagesNew);
		providerInfo.setSearchFilters(searchFilters);
		providerInfo.setProviderAdminGroup(providerAdminGroup);
		providerInfo.setContentDevGroup(contentDevGroup);
		providerInfo.setProviderId(providerId);
		providerInfo.setCourseContainerTemplates(ccTemplates);
		providerInfo.setCourseTemplates(courseTemplates);
		populateDepartmentDetails(request, providerInfo);
		thumbnailParameter = request.getRequestParameter(Constants.COVERIMAGE);

		if ((thumbnailParameter != null) && (thumbnailParameter.getSize() > 0L)) {
			mimeType = thumbnailParameter.getContentType();
			thumbnailStream = thumbnailParameter.getInputStream();
		} else if (StringUtils.isNotBlank(request.getParameter(Constants.COVERIMAGEASSETPATH))) {
			Resource coverImageAssetResource = resourceResolver
					.getResource(request.getParameter(Constants.COVERIMAGEASSETPATH));
			if ((coverImageAssetResource != null) && (!ResourceUtil.isNonExistingResource(coverImageAssetResource))) {
				Asset coverImageAsset = coverImageAssetResource.adaptTo(Asset.class);
				if (coverImageAsset != null) {
					Rendition originalRendition = coverImageAsset.getRendition(Constants.ORIGINAL);
					if (originalRendition != null) {
						mimeType = originalRendition.getMimeType();
						thumbnailStream = originalRendition.getStream();
					}
				}
			}
		}
		try {
			ProviderCreationService providerCreationService = getProviderCreationService(providerType);
			if (operation != null) {
				providerId=providerCreationService.createProvider(providerInfo, resourceResolver, mimeType, thumbnailStream);
				LOG.info("providerSecure value"+providerSecure);
				if("true".equals(providerSecure)) {
					
					String secureparentPath = request.getParameter(Constants.SECUREPARENTPATH);
					String secureProviderAdminGroup[] = request.getParameterValues(Constants.SECUREPROVIDERADMINGROUP);
					String secureContentDevGroup[] = request.getParameterValues(Constants.SECURECONTENTDEVGROUP);
					String secureSearchFilters[] = request.getParameterValues(Constants.SECURESEARCHFILTERS);
					String secureLanguages[] = request.getParameterValues(Constants.SECURELANGUAGES);
					providerInfo.setPath(secureparentPath);
					providerInfo.setLanguages(secureLanguages);
					providerInfo.setSearchFilters(secureSearchFilters);
					providerInfo.setProviderAdminGroup(secureProviderAdminGroup);
					providerInfo.setContentDevGroup(secureContentDevGroup);
					providerInfo.setProviderId(providerId);
					providerCreationService.createProvider(providerInfo, resourceResolver, mimeType, thumbnailStream);
					providerInfo.setPath(parentPath);
				}
			} else {
				providerCreationService.updateProvider(providerInfo, resourceResolver, mimeType, thumbnailStream);
			}
			String homeUrl = Constants.PROVIDERURL + providerInfo.getPath();
			String detailsUrl = Constants.PROVIDERDETAILURL + providerInfo.getPath() + Constants.FORWARDSLASH
					+ providerId + Constants.FORWARDSLASH + providerId;
			res.onCreated(providerInfo.getPath() + Constants.FORWARDSLASH + providerInfo.getTitle());
			res.setStatus(201, i18n.get(Constants.PROVIDERCREATED));
			res.setTitle(i18n.get(Constants.PROVIDERCREATED));
			res.setDescription(i18n.get(Constants.PROVIDERSUCCESSDESCRIPTION));
			res.addRedirectLink(xssAPI.getValidHref(homeUrl), i18n.get(Constants.DONE));
			res.addLink("cq-projects-admin-createproject-edit", xssAPI.getValidHref(detailsUrl),
					i18n.get(Constants.OPENPROVIDER));
			res.send(response, true);
		} catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, "Provider Creation", ExceptionCodes.Exception , "Exception occurred: EXCEPTION while creating Provider : "+e);
			res.send(response, false);
		}
	
	}
	
	
	private void populateDepartmentDetails(final SlingHttpServletRequest request,final ProviderData providerInfo)
	{
		int i = 0,j=0;
		String replaceString,replaceCTString = Constants.EMPTYSTRING;
		boolean departMentExist=true;
		String requestDeparTitle,requestDepartmentName= null;
		DepartmentData departmentData = null;
		ContentType contentType = null;
		List<ContentType> contentTypes = null;
		List<DepartmentData> departments = new ArrayList<DepartmentData>();;
		while(departMentExist)
		{
			replaceString =Constants.EMPTYSTRING+i;
			requestDeparTitle = Constants.DEPARTMENT_TITLE.replace(Constants.X, replaceString);
			requestDepartmentName = Constants.DEPARTMENT_NAME.replace(Constants.X, replaceString);
			if( request.getParameter(requestDeparTitle)!=null)
			{
				departmentData = new DepartmentData();
				departmentData.setTitle(request.getParameter(requestDeparTitle));
				departmentData.setName(request.getParameter(requestDepartmentName));
				departmentData.setItemId(Constants.ITEM+ i);
				boolean contentTypeExist=true;
				String requestCTTitle,requestCTName = null;
				contentTypes = new ArrayList<ContentType>();
				j=0;
				while(contentTypeExist)
				{
					
					replaceCTString = Constants.EMPTYSTRING+j;
					requestCTTitle =  Constants.CONTENTTYPE_TITLE.replace(Constants.X, replaceString).replace(Constants.Y, replaceCTString);
					requestCTName =  Constants.CONTENTTYPE_NAME.replace(Constants.X, replaceString).replace(Constants.Y, replaceCTString);
					if( request.getParameter(requestCTTitle)!=null)
					{
						contentType = new ContentType();
						contentType.setTitle(request.getParameter(requestCTTitle));
						contentType.setName(request.getParameter(requestCTName));
						contentType.setItemId(Constants.ITEM+ j);
						
						contentTypes.add(contentType);
					}
					else
					{
						contentTypeExist = false;
					}
					j++;
					
				}
				departmentData.setContentTypes(contentTypes);
				departments.add(departmentData);
			}
			else
			{
				departMentExist = false;
			}
			i++;
		}
		providerInfo.setDepartments(departments);
	
	}
	
	public ProviderCreationService getProviderCreationService(final String providerType) {
		ProviderCreationService providerCreationServiceRes = null;
		for (final ProviderCreationService providerCreationService : providerCreationServices) {
			if (providerCreationService.isAllowedProviderType(providerType)) {
				providerCreationServiceRes=providerCreationService;
				break;
			}
		}
		return providerCreationServiceRes;
	}
	
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	protected void bind(final ProviderCreationService providerCreationService) {
		if (providerCreationServices == null) {
			providerCreationServices = new ArrayList<ProviderCreationService>();
		}
		providerCreationServices.add(providerCreationService);
	}

	
	protected void unbind(final ProviderCreationService providerCreationService) {
		providerCreationServices.remove(providerCreationService);
	}

}