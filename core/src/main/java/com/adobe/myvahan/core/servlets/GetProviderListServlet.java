package com.apple.learning.corplcms.core.servlets;

import static com.apple.learning.logging.core.splunk.SplunkFormatter.logMethodEntry;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apple.learning.corplcms.assetshare.util.QueryBuilderUtil;
import com.apple.learning.corplcms.core.models.KeyValue;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@Component(service = Servlet.class, property = { "sling.servlet.methods=" + HttpConstants.METHOD_POST,
		"sling.servlet.paths=" + "/bin/corplms/providerList" })
public class GetProviderListServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private final static Logger LOG = LoggerFactory.getLogger(GetProviderListServlet.class);

	public static final String CLASS_NAME = GetProviderListServlet.class.getName();

	@Reference
	private QueryBuilder queryBuilder;

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServerException, IOException {

	}

	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServerException, IOException {
		String methodName = "doPost";
		JsonObject json = new JsonObject();
		LOG.trace(logMethodEntry("Fetching CSS Type List", CLASS_NAME, methodName));
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		Session session = request.getResourceResolver().adaptTo(Session.class);
		Map<String, String> queryMap = QueryBuilderUtil.fetchProviderList("/content/corplcms/providers");
		Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
		SearchResult results = query.getResult();
		LOG.info("Provider List count::" + results.getHits().size());
		json.addProperty("All","all");
		if (null != results && null != results.getHits() && results.getHits().size() > 0) {
			Iterator<Resource> assetResources = results.getResources();
			while (assetResources.hasNext()) {
				Resource assetResource = assetResources.next();
				if (assetResource.adaptTo(ValueMap.class).containsKey("jcr:title")) {
					String providerTitle = assetResource.adaptTo(ValueMap.class).get("jcr:title", String.class);
					String providerId = assetResource.getParent().getName();
					json.addProperty(providerTitle, providerId);

				}
			}
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gson.toJson(json);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(prettyJson);
	}

}