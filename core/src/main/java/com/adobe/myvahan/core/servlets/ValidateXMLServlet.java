package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkConstants.DELETE_OLD_FILES;
import static com.apple.learning.logging.core.splunk.SplunkConstants.WORKFLOW;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logException;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMessage;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;
import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodExit;
import static com.apple.learning.logging.core.utility.SplunkUtil.logExceptionToSplunk;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.servlet.Servlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.apple.learning.assets.core.services.CorpLearningTaxonomyService;
import com.apple.learning.corplcms.assetshare.util.CorpLCMUtil;
import com.apple.learning.corplcms.assetshare.util.QueryBuilderUtil;
import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.core.services.ArchivalService;
import com.apple.learning.corplcms.core.services.CorpLCMSEmailService;
import com.apple.learning.corplcms.core.services.DeleteOldFilesService;
import com.apple.learning.corplcms.core.services.MainfestValidatorService;
import com.apple.learning.logging.core.utility.ExceptionCodes;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.google.gson.JsonObject;


@Component(service = Servlet.class,
property = 
{ SLING_SERVLET_PATHS + "=/bin/corplcms/validatemanifest",
  SLING_SERVLET_METHODS + "=GET" })
public class ValidateXMLServlet extends SlingAllMethodsServlet {

	private final static Logger LOG = LoggerFactory.getLogger(ValidateXMLServlet.class);
	
	public static final String VALIDATEXML = "Validate XML Manifest";
	
	public static final String CLASS_NAME = ValidateXMLServlet.class.getName();
	
	@Reference
	ResourceResolverFactory resourceResolverFactory;

    @Reference 
	private CorpLCMSEmailService corpLCMSEmailService;
    @Reference 
    MainfestValidatorService mainfestValidatorService;
    @Reference 
    private QueryBuilder queryBuilder;
    
    @Reference
	DeleteOldFilesService deleteOldFilesService;
    
  //To be placed in configuration.properties file
  	private static	String schemaversion = "1.2";
  	private static	String schema = "ADL SCORM";
  	private static	String scormType = "sco";
	public static final String ASSET_TYPE = "dam:Asset";		
  	private static boolean isValid = true;
	public static final String DAM_NODE_PATH = "/content";	
  	@Reference
	private ArchivalService archivalService;
  	
	private static final String originalLastModifiedProperty = CorpLearningTaxonomyService.ORIGINAL_NODE_PATH + CorpLearningTaxonomyService.FORWARD_SLASH + CorpLearningTaxonomyService.JCR_CONTENT + CorpLearningTaxonomyService.FORWARD_SLASH + CorpLearningTaxonomyService.PROP_JCR_LAST_MODIFIED;

	
	/** 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		doGet(request, response);
	}

    
	/** 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
		String methodName = "doGet";		

        ResourceResolver resourceResolver = request.getResourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);
		JsonObject emailObject = new JsonObject();

        /* Get Parameters
         * @param path = path you want to run the workflow on
         * @param model = workflow model name you want to run.  Typically found in /etc/workflow/models
         */
		try {
			 RequestParameterMap params = request.getRequestParameterMap();
			 String checkToPushToLMS = null;
			 if(params.getValue("checkToPushToLMS")!=null)
			 {
				 checkToPushToLMS =  params.getValue("checkToPushToLMS").getString();
			 }
			 if(checkToPushToLMS!=null)
			 {
				boolean isValid = inManifestCheckForPushToLMS(request) ;
				if(isValid)
				{
					 response.getWriter().write("success");
				}
				else
				{
					response.getWriter().write("failed");
				}
			 }
			 else
			 {
				 
				String[] uploadedFolders = request.getParameterValues("deleteParentpaths");
			    String path = params.getValue("path").getString();
				String uploadType = params.getValue("uploadType").getString();
				String internalPath = path.substring(0,path.indexOf("/imsmanifest.xml"));
				LOG.info("CorpLCMUtil.getCourseFolderPath( internalPath))" + CorpLCMUtil.getCourseFolderPath( internalPath));
			    if(uploadedFolders!=null && uploadedFolders[0].contains(CorpLCMUtil.getCourseFolderPath( internalPath)))
			    {
		        //String model = params.getValue("model").getString();
		        String coursePath = params.getValue("coursePath").getString();
		        LOG.info("Logged In User ValidateXMLServlet" + session.getUserID());
		        LOG.info("coursePath ValidateXMLServlet" + coursePath);
		        LOG.info("path ValidateXMLServlet" + path);
		   //     resetJcrContent(session, resourceResolver, path);
		        // Create a workflow session 
		        if (path.contains("imsmanifest.xml")) {
					String filePath = path.replace("/jcr:content/renditions/original", "");
			        LOG.info("manifest filePath" + filePath);
			        LOG.info("Course Upload");
					if (resourceResolver.getResource(filePath) != null
							&& !ResourceUtil.isNonExistingResource(resourceResolver.getResource(filePath))) {
						Resource filesResource = resourceResolver.getResource(filePath);
						 LOG.debug("filesResource" + filesResource);
						if (DamUtil.isAsset(filesResource) && filesResource.getPath().contains(".xml")) {
							InputStream xmlContentStream = filesResource.adaptTo(Asset.class).getOriginal().getStream();
							// parsing the xml document
							parseXMLContent(xmlContentStream, filesResource.getPath(), resourceResolver, emailObject, session.getUserID(),coursePath);
						}
					}else {
						 LOG.info("Manifest file is not present in the path" + filePath);
					}
				
					String internalContentPath = internalPath.replace("/dam", "");
					String courseDAMPath = null;
					if(!"replaceFiles".equals(uploadType))
					{
						courseDAMPath =CorpLCMUtil.getCourseDAMPath(resourceResolver, internalContentPath);
					}
					else
					{
						courseDAMPath =CorpLCMUtil.getCourseFolderPath( internalPath);
						CorpLCMUtil.setCoursePushToLMSState(session, internalPath, Constants.LMS_STATUS, null);
						CorpLCMUtil.setCoursePushToLMSState(session, internalPath, Constants.PUSHTOLMSBY, null);
						
					}
					LOG.info("courseDAMPath ValidateXMLServlet" + courseDAMPath);
					//Adding jcr:title			
				
				
					if ("course".equals(uploadType)) {
						List<String> deletedAsset = new ArrayList<String>();
						List<String> emptyFolder = new ArrayList<String>();
						deleteOldFiles(request, session, deletedAsset, emptyFolder);
						CorpLCMUtil.resetPushToLMSStateOnCourseHierarchy(session, courseDAMPath, queryBuilder,deletedAsset,emptyFolder);
						addJcrTitle(session,courseDAMPath);
						
					} else {
						CorpLCMUtil.setAssetPushToLMSStateOnReverseHierarchy(session, courseDAMPath,
								internalPath.concat("/"), Constants.PUSH_TO_LMS_STATUS, null, queryBuilder);
					}
		        }

		        response.getWriter().write("success");
		        }
		      
		    }
		}catch (IOException | ParserConfigurationException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, VALIDATEXML, ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while parsing the manifest xml file" + e);				
		}
       
    }
    
    
	/** 
	 * @param session
	 * @param damPath
	 */
	public  void addJcrTitle(Session session, String damPath) {
		String methodName = "addJcrTitle";
		LOG.debug(logMessage(WORKFLOW, CLASS_NAME, methodName, "Adding jcr title to "+damPath));
		try {
			if (damPath != null)
				{
				if(session.nodeExists(damPath)) {
					javax.jcr.Node jcrContent = session.getNode(damPath);
				if (!jcrContent.hasProperty("jcr:title")) {
					LOG.debug(logMessage(WORKFLOW, CLASS_NAME, methodName, "no title"));

					String coursePath = damPath.replace("/content/dam/", "/content/");
					if(session.nodeExists(coursePath+ "/jcr:content")) {	
						javax.jcr.Node courseNode = session.getNode(coursePath+ "/jcr:content");
						{
							if (courseNode.hasProperty("jcr:title")) {
								LOG.debug(logMessage(VALIDATEXML, CLASS_NAME, methodName, "Adding title to course "+courseNode.getProperty("jcr:title").getValue().toString()));
								jcrContent.setProperty("jcr:title", courseNode.getProperty("jcr:title").getValue().toString());
							}
						}			
					}	
				session.save();
			}
		}
	}
		}
		 catch (RepositoryException e) {
			e.printStackTrace();
			LOG.error("Exception on setting jcr:title on Course Dam Path" + e.getMessage());
		}

	}
    
	
	/** 
	 * @param request
	 * @param session
	 * @param deletedAsset
	 * @param emptyFolder
	 */
	private void deleteOldFiles(SlingHttpServletRequest request, Session session,List<String> deletedAsset,List<String> emptyFolder) {
		String methodName = null;
		try {
			String[] uploadedFolders = request.getParameterValues("deleteParentpaths");
			LOG.debug("uploadedFolders : " + Arrays.toString(uploadedFolders));
			String[] uploadedFiles = request.getParameterValues("currentFileList");
			LOG.debug("uploadedFiles : " + Arrays.toString(uploadedFiles));
			List<String> uploadedFilesList = new ArrayList<String>();
			if (uploadedFiles != null) {
				uploadedFilesList = Arrays.asList(uploadedFiles);
			}
			String deleteExistingAssets = "deleteExistingAssets";
			SearchResult results = deleteOldFilesService.getQueryResults(deleteExistingAssets, uploadedFolders[0], null,
					ASSET_TYPE, null, null, null, CorpLearningTaxonomyService.P_LIMIT_MINUS_ONE, Boolean.FALSE,
					session);
			if(results != null ) {
				Iterator<Resource> resources = results.getResources();
				javax.jcr.Node damNode = session.getNode(DAM_NODE_PATH);
				Workspace workspace = session.getWorkspace();		
				while(resources.hasNext()) {
					
					Resource assetResource = resources.next();
					String path = assetResource.getPath();
					javax.jcr.Node assetNode =assetResource.adaptTo(javax.jcr.Node.class);
					LOG.info("assetNode"+assetNode);
					if(uploadedFiles == null || !uploadedFilesList.contains(path)) {
						LOG.info("!uploadedFilesList.contains : " + path);
						try {
						if(checkassetStateandLastModified(assetResource)) {
							LOG.info("checkassetStateandLastModified : " + path);
							deletedAsset.add(path);
							LOG.debug(logMessage(DELETE_OLD_FILES, CLASS_NAME, methodName,"Starting archival and deletion of the asset at path : " + path));
							String assetStatus = archivalService.getMetadataProperty(assetNode, CorpLearningTaxonomyService.ASSET_STATUS);
							archivalService.deleteAndArchive(path, assetStatus, damNode, workspace, session, CorpLearningTaxonomyService.OPERATION_UPLOAD);
							assetNode.remove();
							session.save();
							}
						}
							 catch (RepositoryException e) {
								 e.printStackTrace();
								LOG.error(logException(DELETE_OLD_FILES, CLASS_NAME, methodName, "RepositoryException occured :" + e));
									logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.RepositoryException , "Exception occurred: while deleting assets : "+ path +e);
							}catch (Exception e) {
								e.printStackTrace();
									LOG.error(logException(DELETE_OLD_FILES, CLASS_NAME, methodName, "Exception occured: " + e));
									logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.Exception , "Exception occurred: while deleting assets : "+path+e);
							}
						} 
					else
					{
						LOG.info("uploadedFilesList.contains : " + path);
					}
					}

				}
			
		
			session.refresh(false);
			String deleteEmptyFolders = "deleteEmptyFolders";
			results = deleteOldFilesService.getQueryResults(deleteEmptyFolders, uploadedFolders[0],
					CorpLearningTaxonomyService.SLING_FOLDER_TYPE,
					CorpLearningTaxonomyService.SLING_ORDERED_FOLDER_TYPE, null, null, null,
					CorpLearningTaxonomyService.P_LIMIT_MINUS_ONE, Boolean.TRUE, session);
		
			if (results != null) {
				Iterator<javax.jcr.Node> nodes = results.getNodes();
				String path = null;
				javax.jcr.Node nextNode = null;
				while (nodes.hasNext()) {
					
					javax.jcr.Node folderNode = nodes.next();
					LOG.info("emptyFolder asset Path" + folderNode.getPath());
					try {
					path=folderNode.getPath();
					LOG.info("path" + path);
					if (folderNode.hasNodes()) {
					if(!isFolderUploaded(uploadedFilesList, path))
					{
						LOG.info("!isFolderUploaded : " + path);
						LOG.info("folderNode.getNodes().getSize() : " + folderNode.getNodes().getSize());
						nextNode = folderNode.getNodes().nextNode();
						LOG.info("!nextNode.getName() : " + nextNode.getName());
						if (folderNode.getNodes().getSize() == 1 && nextNode.getName()
								.equals(CorpLearningTaxonomyService.JCR_CONTENT)) {
							LOG.info("size and have jcr content : " + path);
							LOG.debug(logMessage(DELETE_OLD_FILES, CLASS_NAME, methodName,"Path matched for folders : "+path));
							archivalService.deleteFileOnMount(path, CorpLearningTaxonomyService.STAGING);
							folderNode.remove();
							LOG.info("folderNode removed : " + path);
							session.save();
							LOG.info("folderNode saved : " + path);
						}
						else {
							LOG.debug(logMessage(DELETE_OLD_FILES, CLASS_NAME, methodName,"Path matched for folders : "+path));
							archivalService.deleteFileOnMount(path, CorpLearningTaxonomyService.STAGING);
							emptyFolder.add(folderNode.getPath());
							LOG.info("folderNode else removed : " + path);
							folderNode.remove();
							session.save();
							LOG.info("folderNode else saved : " + path);
						}
					}
					}
					else
					{
						if(!isFolderUploaded(uploadedFilesList, path))
						{
							folderNode.remove();
							session.save();
						}
					}
					} catch (RepositoryException e) {
						LOG.error(logException(DELETE_OLD_FILES, CLASS_NAME, methodName, "RepositoryException occured :" + e));
						logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.RepositoryException , "Exception occurred: while deleting folders : "+path +e);
					}catch (Exception e) {
						LOG.error(logException(DELETE_OLD_FILES, CLASS_NAME, methodName, "Exception occured: " + e));
						logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.Exception , "Exception occurred: while deleting folders : "+path+e);
					}
				}
			}
			LOG.debug("deletedAsset" + deletedAsset);
			LOG.debug("emptyFolder" + emptyFolder);
		} catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, VALIDATEXML, ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while deleting old assets" + e);
		}
	}
	
	
	/** 
	 * @param uploadedFilesList
	 * @param folderPath
	 * @return boolean
	 */
	private boolean isFolderUploaded(List<String> uploadedFilesList,String folderPath)
	{
		boolean result = false;
		String assetPath = null; 
		for (Iterator iterator = uploadedFilesList.iterator(); iterator.hasNext();) {
			assetPath = (String) iterator.next();
			if(assetPath.contains(folderPath))
			{
				result =true;
				break;
			}
			
		}
		return result;
		
	}
	
	/** 
	 * @param assetResource
	 * @return boolean
	 */
	public boolean checkassetStateandLastModified(Resource assetResource) {
		String methodName = "checkassetStateandLastModified";
		LOG.trace(logMethodEntry(DELETE_OLD_FILES, CLASS_NAME, methodName));
		boolean canDelete = true;
		LOG.trace(logMessage(DELETE_OLD_FILES, CLASS_NAME, methodName, "assetPath : " + assetResource.getPath()));
		try {
			ValueMap assetPropertiesMap = assetResource.getValueMap();
			String damAssetState = assetPropertiesMap.get(CorpLearningTaxonomyService.JCR_CONTENT + CorpLearningTaxonomyService.FORWARD_SLASH +CorpLearningTaxonomyService.DAM_ASSET_STATE, String.class);
			if(damAssetState == null || !damAssetState.equals(CorpLearningTaxonomyService.ASSET_PROCESSED)) {
				//get diff in hours
				Calendar originalLastModified = assetPropertiesMap.get(originalLastModifiedProperty, Calendar.class);
				long diff = ((Calendar.getInstance().getTimeInMillis()- originalLastModified.getTimeInMillis())/(1000*60*60)); 
				LOG.debug(logMessage(DELETE_OLD_FILES, CLASS_NAME, methodName, "Dam Asset State : " + damAssetState + " . Difference in hours from currenttime to lastmodified : " + diff));
				//Waiting 8hours for assets to be processed, if not then assuming it is stuck in workflow so we can delete that asset
				if(diff <=8) {
					canDelete =false;
				}
			}
		} catch (Exception e) {
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, DELETE_OLD_FILES, ExceptionCodes.Exception , "Exception occurred: Exception While checking asset state: "+assetResource.getPath() + " : " + e);
		}
		LOG.trace(logMessage(DELETE_OLD_FILES, CLASS_NAME, methodName, "canDelete : "+canDelete));
		LOG.trace(logMethodExit(DELETE_OLD_FILES, CLASS_NAME, methodName));
		return canDelete;
	}

    
    
	/** 
	 * @param request
	 * @return boolean
	 */
	private boolean inManifestCheckForPushToLMS(SlingHttpServletRequest request)
    {
    	boolean result = false;
    	try {
    	RequestParameterMap params = request.getRequestParameterMap();
	    String resourePath = params.getValue("path").getString();
	    LOG.info("resourePath"+ resourePath);
    	Resource manifestResource = request.getResourceResolver().getResource(resourePath.concat(Constants.FORWARDSLASH).concat("imsmanifest.xml"));
    	  LOG.info("manifestResource"+ manifestResource.getPath());
    	List<String> aemPaths = new ArrayList<String>();
    	List<String> failedPaths = new ArrayList<String>();
    	Map<String, String> queryMap = QueryBuilderUtil.fetchAllAssetsQueryMap(resourePath);
		Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), request.getResourceResolver().adaptTo(Session.class));
		SearchResult results = query.getResult();
		if (null != results && null != results.getHits() && results.getHits().size() > 0) {
			Iterator<Resource> assetResources = results.getResources();
			while (assetResources.hasNext()) {
				Resource assetResource = assetResources.next();
				if(!assetResource.getPath().endsWith("imsmanifest.xml"))
				{
					aemPaths.add(CorpLCMUtil.getAEMSuffixPath(assetResource.getPath()));
				}
			}
		}
		LOG.info("aemPaths  inManifestCheckForPushToLMS " + aemPaths);
    
    	 result =mainfestValidatorService.doValidation(resourePath, manifestResource, aemPaths, false, failedPaths);
    	 LOG.info("manifestValidationResult"+ result);
		
    	}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return result;
    }
    
  
    
	/** 
	 * @param xmlContentStream
	 * @param path
	 * @param resolver
	 * @param emailObject
	 * @param loggedInUser
	 * @param coursePath
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private void parseXMLContent(InputStream xmlContentStream, String path, ResourceResolver resolver, JsonObject emailObject,String loggedInUser,String coursePath)
			throws IOException, ParserConfigurationException {
		String methodName = "parseXMLContent";
		ArrayList<String> assetdetails = new ArrayList<String>();
		String courseId = "";
	    String courseTitle = "";
	    String courseType = "";
	    String version = "";
	    String locale = "";
	    SimpleDateFormat gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

	    //Current Date Time in GMT
	    String uploadDate = gmtDateFormat.format(new Date())+ " GMT";

		try {
			LOG.info(logMethodEntry(VALIDATEXML, CLASS_NAME, methodName));
			//Getting course details
			  
			String[] splitPath =  path.split("/byoc/");
		    String courseLocalePath = splitPath[0];
		    locale = courseLocalePath.substring(courseLocalePath.lastIndexOf("/")+1);
		    if(null != coursePath && !coursePath.isEmpty()) {			 
					 if(!ResourceUtil.isNonExistingResource(resolver.getResource(coursePath+"/jcr:content"))){
						 Resource courseResource = resolver.getResource(coursePath+"/jcr:content");
						 javax.jcr.Node courseNode = courseResource.adaptTo(javax.jcr.Node.class);
						 if(null != courseNode ) {
							 if(courseNode.hasProperty("jcr:title")){
								 courseTitle =  courseNode.getProperty("jcr:title").getValue().toString();								 
							 }
							 if(courseNode.hasProperty("corplcms:lmsVersionLabel")){
								 version =  courseNode.getProperty("corplcms:lmsVersionLabel").getValue().toString();								 
							 }
							 if(courseNode.hasProperty("cq:template")){
								 courseType =  courseNode.getProperty("cq:template").getValue().toString();								 
							 }
							 if(courseNode.hasProperty("corplcms:courseId")){
								 courseId =  courseNode.getProperty("corplcms:courseId").getValue().toString();								 
							 }
						 }
					 }
				 }
				 if(null != courseType && !courseType.isEmpty()) {
					 if(courseType.contains("BYOC")) {
						 courseType = "BYOC";							 
					 }
				 }
			// get XML document from the XML file input stream

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//	factory.setValidating(true);
			//Added to fix security issue - java:S2755
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

			DocumentBuilder builder = factory.newDocumentBuilder();

			Document xmlDocument = builder.parse(xmlContentStream);
			xmlDocument.getDocumentElement().normalize();
           
			NodeList elementsList = xmlDocument.getElementsByTagName("manifest");
			if(elementsList.getLength()>0) {

			Element documentElements = xmlDocument.getDocumentElement();
		    NodeList nodeList = documentElements.getChildNodes();
		    for (int i = 0; i < nodeList.getLength(); i++) {
		        if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
		            Element element = (Element) nodeList.item(i);
		            if (!element.getNodeName().contains("resources") && element.getNodeName().contains("metadata")) {
		                String xmlSchema = element.getElementsByTagName("schema").item(0).getTextContent();
		                if (xmlSchema !="" && !xmlSchema.equalsIgnoreCase(schema)) {
		                	emailObject.addProperty("schema",xmlSchema);
		    				isValid = false;
		    			}
		                String xmlSchemaVersion = element.getElementsByTagName("schemaversion").item(0).getTextContent();
		                if (xmlSchemaVersion != "" && !xmlSchemaVersion.equalsIgnoreCase(schemaversion)) {
		                	emailObject.addProperty("schemaversion",xmlSchemaVersion);
		    				isValid = false;
		    			}
		            }
		        }
		    }
			LOG.info("Manifest file path "+path);
			NodeList resourceElementsList = xmlDocument.getElementsByTagName("resource");
			if (resourceElementsList.getLength() > 0) {
				for (int temp = 0; temp < resourceElementsList.getLength(); temp++) {
					ArrayList<String> errorPath = new ArrayList<String>();
					Node listElementNode = resourceElementsList.item(temp);

					if (listElementNode.getNodeType() == Node.ELEMENT_NODE) {
						Element listElement = (Element) listElementNode;
						String xmlResourceAttributeValue = listElement.getAttribute("href");
						errorPath = verifyResourceTagElements(listElement, true, path, resolver,emailObject);
						assetdetails.addAll(errorPath);
					}
				}
				if (!assetdetails.isEmpty()) {
					 LOG.info("Sending error mail");
					 assetdetails.add(0,"Course ID : "+courseId);
					 assetdetails.add(1,"CMS Course Title : "+courseTitle);
					 assetdetails.add(2,"Course Locale : "+locale);
					 assetdetails.add(3,"CMS Version : "+version);
					 assetdetails.add(4,"Course Type : "+courseType);
					 assetdetails.add(5,"Course Uploaded By : "+loggedInUser);
					 assetdetails.add(6,"Course Uploaded Date : "+uploadDate);
					 corpLCMSEmailService.sendMail(assetdetails,"failure","Course Upload Failure - "+courseId,loggedInUser);
				 }else {
					 LOG.info("Manifest file validation is Success");
				 }
			}		
			}
			LOG.info("Exit parseXMLContent(), MANIFEST FILE is valid :{}", isValid);
		} catch (SAXException e) {
			//send email notification
			 LOG.info("Sending error mail due to SAXException");
			 assetdetails.add(0,"Course ID : "+courseId);
			 assetdetails.add(1,"CMS Course Title : "+courseTitle);
			 assetdetails.add(2,"Course Locale : "+locale);
			 assetdetails.add(3,"CMS Version : "+version);
			 assetdetails.add(4,"Course Type : "+courseType);
			 assetdetails.add(5,"Course Uploaded By : "+loggedInUser);
			 assetdetails.add(6,"Course Uploaded Date : "+uploadDate);
			 assetdetails.add(7,"");
			 assetdetails.add(8,"Failure Details : Invalid Manifest File");
			 
			 corpLCMSEmailService.sendMail(assetdetails,"failure","Course Upload Failure - "+courseId,loggedInUser);

		 	logExceptionToSplunk(LOG, CLASS_NAME, methodName, VALIDATEXML, ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while parsing the manifest xml file" + e);	
		} catch (ParserConfigurationException e) {
			//send email notification
			 LOG.info("Sending error mail due to ParserConfigurationException");
			 assetdetails.add(0,"Course ID : "+courseId);
			 assetdetails.add(1,"CMS Course Title : "+courseTitle);
			 assetdetails.add(2,"Course Locale : "+locale);
			 assetdetails.add(3,"CMS Version : "+version);
			 assetdetails.add(4,"Course Type : "+courseType);
			 assetdetails.add(5,"Course Uploaded By : "+loggedInUser);
			 assetdetails.add(6,"Course Uploaded Date : "+uploadDate);
			 assetdetails.add(7,"");
			 assetdetails.add(8,"Failure Details : Invalid Manifest File");
			 
			 corpLCMSEmailService.sendMail(assetdetails,"failure","Course Upload Failure - "+courseId,loggedInUser);

			logExceptionToSplunk(LOG, CLASS_NAME, methodName, VALIDATEXML, ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while parsing the manifest xml file" + e);	
		} catch (RepositoryException e) {
			//send email notification
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, VALIDATEXML, ExceptionCodes.RepositoryException,
					"Exception occurred: EXCEPTION while parsing the manifest xml file" + e);	
		} catch (IOException e) {
			//send email notification
			logExceptionToSplunk(LOG, CLASS_NAME, methodName, VALIDATEXML, ExceptionCodes.Exception,
					"Exception occurred: EXCEPTION while parsing the manifest xml file" + e);	
		}
	}

	
	
	/** 
	 * @param element
	 * @param isValid
	 * @param path
	 * @param resolver
	 * @param emailObject
	 * @return ArrayList<String>
	 */
	private ArrayList<String> verifyResourceTagElements(Element element,boolean isValid, String path, ResourceResolver resolver,JsonObject emailObject) {
		String methodName = "ResourcePathVerification";
		ArrayList<String> assetpath = new ArrayList<String>();
		String xmlScormTypeAttributeValue = element.getAttribute("adlcp:scormtype");
		if(!xmlScormTypeAttributeValue.equalsIgnoreCase(scormType)) {
			emailObject.addProperty("scormType",xmlScormTypeAttributeValue);
			isValid = false;
		}
		NodeList fileElementsList = element.getElementsByTagName("file");
		if(fileElementsList.getLength() > 0){
			
			for (int temp = 0; temp < fileElementsList.getLength(); temp++) {
				Node fileListElementNode = fileElementsList.item(temp);
				if (fileListElementNode.getNodeType() == Node.ELEMENT_NODE) {
					Element listElement = (Element) fileListElementNode;
				    String filehrefAttributeValue = listElement.getAttribute("href");				    
				    if (filehrefAttributeValue.contains("%20")) {
				    	filehrefAttributeValue = filehrefAttributeValue.replaceAll("%20", " ");
				    }
				   // String resourcePath = path + filehrefAttributeValue;
				    //iterate all the file tag href and check if resource is present

				    String[] splitPath =  path.split("/byoc/");
				    String coursename = splitPath[1];
				    String pathh = splitPath[0]+"/byoc/"+coursename.substring(0,coursename.indexOf("/", coursename.indexOf("/") + 1)+1);	
				    String resourcepath = pathh + filehrefAttributeValue;
				    try {
				    if (resolver.getResource(resourcepath) == null) {
				    		isValid = false;
				    	LOG.debug("Exit parseXMLContent(), manifestFile validation failed at :{} resource path", resourcepath);
				    	if(null != resourcepath && !resourcepath.isEmpty()) {
				    		resourcepath = resourcepath.split("/byoc/")[1];
				    		resourcepath = resourcepath.substring(resourcepath.indexOf('/') + 1);
				    	}
				    	assetpath.add(resourcepath);
				    }/* else if(!ResourceUtil.isNonExistingResource(resolver.getResource(resourcepath))) {
				    		isValid = true;
				    	}*/
				    } catch(NullPointerException e) { 
				    	logExceptionToSplunk(LOG, CLASS_NAME, methodName, VALIDATEXML, ExceptionCodes.Exception,
								"Exception occurred: EXCEPTION while parsing the manifest xml file" + e);				
			        } 
				
				}
			}
		}
		return assetpath;
	}
}