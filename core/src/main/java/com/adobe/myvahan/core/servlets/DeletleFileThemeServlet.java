package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;
import java.io.IOException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.util.CommonUtil;

import lombok.Synchronized;

@Component(service = Servlet.class, immediate = true, enabled = true, property = {
		SLING_SERVLET_PATHS + "=/bin/corplcms/theme/file/delete", SLING_SERVLET_METHODS + "=POST" })
public class DeletleFileThemeServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = -1238148292770358629L;
	private static final Logger LOG = LoggerFactory.getLogger(ApplyThemeServlet.class);
	private transient ResourceResolver resolver;
	@Reference
	private transient ResourceResolverFactory resolverFactory;

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

	@Override
	@Synchronized
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		String methodName = "doPost";
		LOG.debug("In  method {}", methodName);
		String themePath = "";
		String assetPath = request.getParameter("assetPath");
		String extension = assetPath.substring(assetPath.lastIndexOf(".") + 1);

		try {
			resolver = CommonUtil.getServiceResourceResolver(null, resolverFactory);
			if (assetPath.contains("/default/")) {

				themePath = assetPath.substring(0, assetPath.indexOf("/default/") + 8);

			} else {
				themePath = assetPath.substring(0, assetPath.indexOf("/pr-") + 9);
			}
			if (resolver != null) {
				Resource themeResource = resolver.getResource(themePath);
				if (themeResource != null) {
					Resource jcrthemeResource = themeResource.getChild("jcr:content");
					if (jcrthemeResource != null) {
						Resource styleGroupRes = jcrthemeResource.getChild("cq:styleGroups");
						if (styleGroupRes != null) {
							Node styleGroupNode = styleGroupRes.adaptTo(Node.class);
							if (extension.equals("css")) {

								if (styleGroupNode.hasProperty("cq:cssStyles")) {
									updateStyleNode(styleGroupNode, assetPath, "cq:cssStyles");

								}
							} else if (extension.equals("js")) {
								if (styleGroupNode.hasProperty("cq:jsStyles")) {
									updateStyleNode(styleGroupNode, assetPath, "cq:jsStyles");

								}
							}

						}
						ModifiableValueMap contentProperties = jcrthemeResource.adaptTo(ModifiableValueMap.class);
						contentProperties.put("status", "modified");
					}
				}
			}
			response.setStatus(200);
		} catch (Exception e) {
			LOG.error("Exception while updating property in delete file:{}", e);
			response.setStatus(500);
		} finally {
			resolver.commit();
		}
	}

	public static void updateStyleNode(Node styleGroupNode, String assetPath, String properTyName) {
		try {
			Value[] styleFiles = null;
			if (styleGroupNode.getProperty(properTyName).isMultiple()) {

				styleFiles = styleGroupNode.getProperty(properTyName).getValues();

			} else {

				styleFiles = new Value[] { styleGroupNode.getProperty(properTyName).getValue() };

			}
			if (styleFiles.length == 1) {
				styleGroupNode.setProperty(properTyName, (Value) null);
			} else {
				String[] newStyleFiles = new String[styleFiles.length - 1];
				int index = 0;
				for (Value v : styleFiles) {
					if (!v.getString().equalsIgnoreCase(assetPath)) {
						newStyleFiles[index] = v.getString();
						if (index == styleFiles.length - 1) {
							break;
						}
						index++;
					}
				}

				styleGroupNode.setProperty(properTyName, newStyleFiles);
			}
		} catch (ValueFormatException e) {
			LOG.error("Exception while updating property"+properTyName+" in delete file:{}", e);
		} catch (IllegalStateException e) {
			LOG.error("Exception while updating property"+properTyName+" in delete file:{}", e);
		} catch (RepositoryException e) {
			LOG.error("Exception while updating property"+properTyName+" in delete file:{}", e);
		}

	}

}
