package com.adobe.myvahan.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Common Node Property Update Configuration", description = "This configuration contains the details related to Node property update")
public @interface CommonNodePropertyUpdateConfiguration {

	@AttributeDefinition(name = "Node Path List", description = "Node Path List to update Property")
	String[] getNodePathList() default {};

	@AttributeDefinition(name = "modify property list", description = "List of existing properties to modify and mention in key|value pair")
	String[] getModifyPropertyList() default {
			"cq:allowedTemplates|/conf/corp-lcms/settings/wcm/templates/BYOC-template,/conf/corp-lcms-url-courses/settings/wcm/templates/url-course",
			"jcr:title|External" };

	@AttributeDefinition(name = "content-foder and domainID list", description = "content-folder and domainID list for all the providers key|value pair")
	String[] getContentFolderAndDomainIDList() default {
			"PR001|Services|domin000000098480812|External|cnfld000000002006826",
			"PR002|AppleMusic & International|domin000000075467949|External|cnfld000000002006800" };

	@AttributeDefinition(name = "Add New property list", description = "List of New properties to Add and mention in key|value pair")
	String[] getNewPropertyList() default {};

	@AttributeDefinition(name = "Delete Existing property list", description = "List of Existing properties to Delete and mention in key|value pair")
	String[] getDeletePropertyList() default {};

}
