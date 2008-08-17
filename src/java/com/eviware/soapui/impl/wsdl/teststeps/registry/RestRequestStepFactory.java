/*
 *  soapUI, copyright (C) 2004-2008 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl.teststeps.registry;

import java.util.ArrayList;
import java.util.List;

import com.eviware.soapui.config.CredentialsConfig;
import com.eviware.soapui.config.RestRequestConfig;
import com.eviware.soapui.config.RestRequestStepConfig;
import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.rest.RestRequest;
import com.eviware.soapui.impl.rest.RestResource;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;

/**
 * Factory for WsdlTestRequestSteps
 * 
 * @author Ole.Matzura
 */

public class RestRequestStepFactory extends WsdlTestStepFactory
{
	public static final String RESTREQUEST_TYPE = "restrequest";
	public static final String STEP_NAME = "Name";
	private XFormDialog dialog;
	private StringToStringMap dialogValues = new StringToStringMap();

	public RestRequestStepFactory()
	{
		super( RESTREQUEST_TYPE, "REST Test Request", "Submits a REST-style Request and validates its response", "/request.gif" );
	}

	public WsdlTestStep buildTestStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest)
	{
		return new RestTestRequestStep( testCase, config, true, forLoadTest );
	}

	public static TestStepConfig createConfig(RestRequest request, String stepName)
	{
		RestRequestStepConfig requestStepConfig = RestRequestStepConfig.Factory.newInstance();
		
      requestStepConfig.setService( request.getOperation().getInterface().getName() );
      requestStepConfig.setResourcePath( request.getOperation().getFullPath() );

      RestRequestConfig testRequestConfig = requestStepConfig.addNewRestRequest();
      
      testRequestConfig.setName( stepName );
      testRequestConfig.setEncoding( request.getEncoding() );
      testRequestConfig.setEndpoint( request.getEndpoint() );
      testRequestConfig.addNewRequest().setStringValue( request.getRequestContent() );

      if( (CredentialsConfig) request.getConfig().getCredentials() != null )
      {
      	testRequestConfig.setCredentials( (CredentialsConfig) request.getConfig().getCredentials().copy() );
      }

      TestStepConfig testStep = TestStepConfig.Factory.newInstance();
      testStep.setType( RESTREQUEST_TYPE );
      testStep.setConfig( requestStepConfig );

		return testStep;
	}

	public static TestStepConfig createConfig( RestResource operation, String stepName )
	{
		RestRequestStepConfig requestStepConfig = RestRequestStepConfig.Factory.newInstance();
		
      requestStepConfig.setService( operation.getInterface().getName() );
      requestStepConfig.setResourcePath( operation.getFullPath() );

      RestRequestConfig testRequestConfig = requestStepConfig.addNewRestRequest();
      
      testRequestConfig.setName( stepName );
      testRequestConfig.setEncoding( "UTF-8" );
      String[] endpoints = operation.getInterface().getEndpoints();
      if( endpoints.length > 0 )
      	testRequestConfig.setEndpoint( endpoints[0] );
      
      testRequestConfig.addNewRequest();
      
      TestStepConfig testStep = TestStepConfig.Factory.newInstance();
      testStep.setType( RESTREQUEST_TYPE );
      testStep.setConfig( requestStepConfig );

		return testStep;
	}
	
	public TestStepConfig createNewTestStep(WsdlTestCase testCase, String name)
	{
		// build list of available interfaces / operations
		Project project = testCase.getTestSuite().getProject();
		List<String> options = new ArrayList<String>();
		List<RestResource> operations = new ArrayList<RestResource>();

		for( int c = 0; c < project.getInterfaceCount(); c++ )
		{
			Interface iface = project.getInterfaceAt( c );
			if( iface instanceof RestService )
			{
				RestResource[] resources = ((RestService)iface).getAllResources();
				
				for( RestResource resource : resources )
				{
					options.add( iface.getName() + " -> " + resource.getFullPath() );
					operations.add( resource );
				}
			}
		}
		
		Object op = UISupport.prompt( "Select Resource to invoke for request", "New RestRequest", options.toArray() );
		if( op != null )
		{
			int ix = options.indexOf( op );
			if( ix != -1 )
			{
				RestResource operation = operations.get( ix );
				
				if( dialog == null )
					buildDialog();
				
				dialogValues.put( STEP_NAME, name );
				dialogValues = dialog.show( dialogValues );
				if( dialog.getReturnValue() != XFormDialog.OK_OPTION )
					return null;

				return createNewTestStep(operation, dialogValues);
			}
		}
		
		return null;
	}

	public TestStepConfig createNewTestStep(RestResource operation, StringToStringMap values )
	{
		String name = values.get( STEP_NAME );
		
		RestRequestStepConfig requestStepConfig = RestRequestStepConfig.Factory.newInstance();
		
		requestStepConfig.setService( operation.getInterface().getName() );
		requestStepConfig.setResourcePath( operation.getName() );

		RestRequestConfig testRequestConfig = requestStepConfig.addNewRestRequest();
		
		testRequestConfig.setName( name );
		testRequestConfig.setEncoding( "UTF-8" );
		String[] endpoints = operation.getInterface().getEndpoints();
		if( endpoints.length > 0 )
			testRequestConfig.setEndpoint( endpoints[0] );
		
		TestStepConfig testStep = TestStepConfig.Factory.newInstance();
		testStep.setType( RESTREQUEST_TYPE );
		testStep.setConfig( requestStepConfig );
		testStep.setName( name );

		return testStep;
	}

	public boolean canCreate()
	{
		return true;
	}
	
	private void buildDialog()
	{
		XFormDialogBuilder builder = XFormFactory.createDialogBuilder("Add REST Request to TestCase");
		XForm mainForm = builder.createForm( "Basic" );
		
		mainForm.addTextField( STEP_NAME, "Name of TestStep", XForm.FieldType.URL ).setWidth( 30 );

		dialog = builder.buildDialog( builder.buildOkCancelActions(), 
				"Specify options for adding a new REST Request to a TestCase", UISupport.OPTIONS_ICON);		
	}
}
