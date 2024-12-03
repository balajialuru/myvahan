package com.adobe.myvahan.core.servlets;

import com.adobe.myvahan.core.services.CommonNodePropertyUpdateConfigurationService;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.Replicator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.servlet.Servlet;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.regex.Pattern;

@Component(service = Servlet.class, property = { "sling.servlet.paths=/bin/nodeupdate",
		"sling.servlet.methods=" + HttpConstants.METHOD_GET })
public class CommonNodePropertyCreateUpdateServlet extends SlingAllMethodsServlet {
	private static final long serialVersionUID = 1L;

	@Reference
	private ResourceResolverFactory factory;

	@Reference
	CommonNodePropertyUpdateConfigurationService commonNodePropertyUpdateConfigurationService;

	@Reference
	private Replicator replicator;

	javax.jcr.Session session;

	ResourceResolver resourceResolver;

	List<String> nodePathList;
	

	private final static Logger LOG = LoggerFactory.getLogger(CommonNodePropertyCreateUpdateServlet.class);
	//private static final String CLASS_NAME = ProviderUtil.class.getName();

	@Override
	protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse resp)
			throws IOException {
		
		try {
			resp.setContentType("text/html");
			resourceResolver = request.getResourceResolver();
			session = resourceResolver.adaptTo(Session.class);

			nodePathList = Arrays.asList(commonNodePropertyUpdateConfigurationService.getNodePathList());

			updateModifiedProperties(nodePathList, resp);

			resp.getWriter().write("All the properties are saved");

		} catch (Exception e) {
			e.printStackTrace();
			resp.setStatus(500);
			resp.getWriter().write("Failed to save.");
		} finally {
			if (session != null && session.isLive()) {
				session.logout();
			}

			if (resourceResolver != null && resourceResolver.isLive()) {
				resourceResolver.close();
			}
		}
	}

	private void updateModifiedProperties(List<String> nodePathList, SlingHttpServletResponse resp) throws PathNotFoundException, RepositoryException,
			ValueFormatException, VersionException, LockException, ConstraintViolationException, AccessDeniedException,
			ItemExistsException, ReferentialIntegrityException, InvalidItemStateException, NoSuchNodeTypeException, IOException {
		LOG.debug("inside updateModifiedProperties method.." );
		if (Objects.nonNull(nodePathList) && !nodePathList.isEmpty()) {

			ListIterator<String> listIterator = nodePathList.listIterator();
			int i = 0;
			int count = 0;
			String nodePath=null;
			while (listIterator.hasNext()) {
				

				nodePath = listIterator.next();
				Resource pathResource = resourceResolver.getResource(nodePath);
				if(Objects.nonNull(pathResource)) {
					updateNodeProperty(pathResource);
						i++;
						
						if(i == 100) {
							resourceResolver.commit();
							LOG.debug("<-------------- {} paths updated ---------->",i);
							i=0;
						}
										
						LOG.debug(">>>>>>>>property updated for path ::" + nodePath);
						resp.getWriter().write(">>>>>>>>property updated for path ::" + nodePath);
						resp.getWriter().write("<br/>");
						count++;
				}
				else {
					LOG.debug(">>>>>>>>Resource does not exist ::" + nodePath);
					resp.getWriter().write("Resource does not exist ::" + nodePath);
					resp.getWriter().write("<br/>");
				}
				
				
			}
			resourceResolver.commit();
			LOG.debug(">>>>>>>>Total >>> {} <<<< number of paths updated>>>>>>>>>>",count);
			resp.getWriter().write(">>>>>>>>Total number of paths updated >>>>>>>>>>" + count);
			
		}
	}

	private void updateNodeProperty(Resource pathResource) {
		ModifiableValueMap modifiableValueMap = pathResource.adaptTo(ModifiableValueMap.class);
		
		//	Node sourceNode = session.getNode(nodePath + "/jcr:content");
			String[] modfiyProperites = commonNodePropertyUpdateConfigurationService.getModifyPropertyList();

			Map<String, String> propertiesMap = getListOfModifyProperties(modfiyProperites);
			for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();

				// to update multivalued Property
				if (value.contains(",")) {
					String[] values = value.split(",");
					
					if (modifiableValueMap.containsKey(key) && modifiableValueMap.get(key).getClass().isArray()) {
						modifiableValueMap.put(key, values);
					} else {
						modifiableValueMap.remove(key);
						modifiableValueMap.put(key, values);
					}
					
				//	sourceNode.setProperty(key, values);
				}
				// to update single property
				else {
					modifiableValueMap.put(key, value );
			//		sourceNode.setProperty(key, value);
				}

			}
	}

	
	
	protected Map<String, String> getListOfModifyProperties(String[] modfiyProperites) {
		Map<String, String> propertiesMap = new LinkedHashMap<String, String>();
		if (Objects.nonNull(modfiyProperites) && modfiyProperites.length > 0) {
			for (String property : modfiyProperites) {
				String[] splitKeyValues = property.split(Pattern.quote("|"));
				if (splitKeyValues.length == 2) {
					String key = splitKeyValues[0];
					String Value = splitKeyValues[1];
					propertiesMap.put(key, Value);
				}
			}
		}
		return propertiesMap;
	}
	

	protected void addProperty() {

	}

	protected void deleteProperty() {

	}

	protected void addNode() {

	}

}
