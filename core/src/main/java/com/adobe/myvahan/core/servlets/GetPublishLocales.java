package com.apple.learning.corplcms.core.servlets;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_METHODS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;
import java.util.Arrays;

import javax.jcr.Node;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.apple.learning.corplcms.constants.Constants;
import com.apple.learning.corplcms.util.RegionsEnum;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { SLING_SERVLET_PATHS + "=/bin/corplms/getpublishlocales",
		SLING_SERVLET_METHODS + "=GET" })

public class GetPublishLocales extends SlingAllMethodsServlet {

	@Reference
	private ResourceResolverFactory resolverFactory;
	/**
	 * 
	 */
	private static final long serialVersionUID = -4332207652412253871L;

	
	/** 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {
		String pagePath = request.getParameter("currentPath");
		pagePath = pagePath.replace(".html", "");
		JsonObject json = new JsonObject();

		Resource PageContentRes = request.getResourceResolver().getResource(pagePath + "/jcr:content");
		
		if(null!=PageContentRes) {
			ValueMap vm = PageContentRes.getValueMap();
			String pageDamPath = vm.get("corplcms:damPath", String.class) != null ? vm.get("corplcms:damPath", String.class)
					: "/content/dam/corplcms";

			int lastIndex = pagePath.lastIndexOf(Constants.FORWARDSLASH);

			String[] pathSplits = pagePath.split(Constants.FORWARDSLASH);
			int initiativeIndex = 5;
			String provider = String.join(Constants.FORWARDSLASH, Arrays.copyOfRange(pathSplits, 0, initiativeIndex));
			lastIndex = provider.lastIndexOf(Constants.FORWARDSLASH);
			Resource resource = request.getResourceResolver()
					.getResource(provider + "/" + provider.substring(lastIndex + 1) + "/jcr:content");
			ValueMap property = resource.adaptTo(ValueMap.class);
			String[] locales = property.get("corplcms:languages", String[].class);
			String globalDamPath = pageDamPath;
			if (!pageDamPath.contains("/GLOBAL/")) {
				String[] localPathSplits = pageDamPath.split("/");
				provider = provider.replace("/content/corplcms", "/content/dam/corplcms");
				globalDamPath = provider + "/GLOBAL/en_Global/"
						+ String.join("/", Arrays.copyOfRange(localPathSplits, 9, localPathSplits.length));
			}

			String[] globalPathSplits = globalDamPath.split("/");
			String dampath = String.join("/", Arrays.copyOfRange(globalPathSplits, 0, 6));
			String restDampath = String.join("/", Arrays.copyOfRange(globalPathSplits, 8, globalPathSplits.length));
			String val = "";
			if (resource.getResourceResolver().getResource(globalDamPath) != null) {
				json.addProperty("en_Global", "en_Global");
			}
			for (String locale : locales) {
				lastIndex = locale.lastIndexOf(Constants.FORWARDSLASH);
				String localeInernal = locale.substring(lastIndex + 1);
				String countryCode = locale.split("_")[1];
//				val = dampath + "/" + RegionsEnum.valueOf(locale).regionalCode().toString() + "/" + countryCode + "/"
//						+ locale + "/" + restDampath;
				
				val = dampath + "/" + getRegionName(request.getResourceResolver(),locale) + "/" + countryCode + "/"
						+ localeInernal + "/" + restDampath;

				if (resource.getResourceResolver().getResource(val) != null) {

					json.addProperty(localeInernal, localeInernal);

				}
			}
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(prettyJson);
	}
	
	private String getRegionName(ResourceResolver resolver,String locale) {
		String region=null;
		try {
		TagManager tagMgr = resolver.adaptTo(TagManager.class);
		//corplcms:locale/BR/pt_BR
		
		String localeTagPath = locale.replace("corplcms:", "/content/cq:tags/corplcms/");
		Tag localeTag = tagMgr.resolve(localeTagPath);
		
		Node localeNode = localeTag.adaptTo(Node.class);
		
		region = localeNode.getProperty("region").getString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return region;
	}
	
	
}
