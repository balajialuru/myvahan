package com.adobe.myvahan.core.services.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;


import com.adobe.myvahan.core.config.CommonNodePropertyUpdateConfiguration;
import com.adobe.myvahan.core.services.CommonNodePropertyUpdateConfigurationService;

@Component(service = CommonNodePropertyUpdateConfigurationService.class, immediate = true)
@Designate(ocd = CommonNodePropertyUpdateConfiguration.class)
public class CommonNodePropertyUpdateConfigurationServiceImpl implements CommonNodePropertyUpdateConfigurationService{
	
	private String[] nodeList;
	private String[] modifyPropertyList;
	private String[] newPropertyList;
	private String[] deletePropertyList;
	private String[] contentFolderAndDomainIDList;

	@Activate
	public void activate(CommonNodePropertyUpdateConfiguration config) {
		this.nodeList = config.getNodePathList();
		this.modifyPropertyList = config.getModifyPropertyList();
		this.newPropertyList = config.getNewPropertyList();
		this.deletePropertyList = config.getDeletePropertyList();
		this.contentFolderAndDomainIDList = config.getContentFolderAndDomainIDList();
	}

	@Override
	public String[] getNodePathList() {
		return nodeList;
	}

	@Override
	public String[] getModifyPropertyList() {
		return modifyPropertyList;
	}

	@Override
	public String[] getNewPropertyList() {
		return newPropertyList;
	}

	@Override
	public String[] getDeletePropertyList() {
		return deletePropertyList;
	}

	@Override
	public String[] getContentFolderAndDomainIDList() {
		// TODO Auto-generated method stub
		return contentFolderAndDomainIDList;
	}

}
