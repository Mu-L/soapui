/*
 *  soapUI, copyright (C) 2004-2011 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.security.check;

import javax.swing.JComponent;

import com.eviware.soapui.config.SQLInjectionCheckConfig;
import com.eviware.soapui.config.SecurityCheckConfig;
import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCaseRunner;
import com.eviware.soapui.impl.wsdl.teststeps.HttpResponseMessageExchange;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestInterface;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.testsuite.SamplerTestStep;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestRunner.Status;
import com.eviware.soapui.security.SecurityCheckRequestResult;
import com.eviware.soapui.security.SecurityTestRunContext;
import com.eviware.soapui.security.SecurityCheckRequestResult.SecurityCheckStatus;
import com.eviware.soapui.security.fuzzer.Fuzzer;
import com.eviware.soapui.security.log.SecurityTestLogMessageEntry;
import com.eviware.soapui.security.log.SecurityTestLogModel;
import com.eviware.soapui.security.ui.SecurityCheckConfigPanel;
import com.eviware.soapui.support.SecurityCheckUtil;
import com.eviware.soapui.support.types.StringToObjectMap;

/**
 * This will test whether a targeted web page is vulnerable to reflected XSS
 * attacks
 * 
 * @author soapui team
 */

public class SQLInjectionCheck extends AbstractSecurityCheck implements SensitiveInformationCheckable
{

	public static final String TYPE = "SQLInjectionCheck";

	public SQLInjectionCheck( SecurityCheckConfig config, ModelItem parent, String icon, TestStep testStep )
	{
		super( testStep, config, parent, icon );
		if( config == null )
		{
			config = SecurityCheckConfig.Factory.newInstance();
			SQLInjectionCheckConfig pescc = SQLInjectionCheckConfig.Factory.newInstance();
			config.setConfig( pescc );
		}
		if( config.getConfig() == null )
		{
			SQLInjectionCheckConfig pescc = SQLInjectionCheckConfig.Factory.newInstance();
			config.setConfig( pescc );
		}
	}

	protected SecurityCheckRequestResult execute( TestStep testStep, SecurityTestRunContext context, SecurityTestLogModel securityTestLog,
			SecurityCheckRequestResult securityChekResult )
	{
		if( acceptsTestStep( testStep ) )
		{
			WsdlTestCaseRunner testCaseRunner = new WsdlTestCaseRunner( ( WsdlTestCase )testStep.getTestCase(),
					new StringToObjectMap() );

			String originalResponse = getOriginalResult( testCaseRunner, testStep ).getResponse().getContentAsXml();

			if( getExecutionStrategy().equals( SecurityCheckParameterSelector.SEPARATE_REQUEST_STRATEGY ) )
			{
				for( String param : getParamsToCheck() )
				{
					Fuzzer sqlFuzzer = Fuzzer.getSQLFuzzer();

					while( sqlFuzzer.hasNext() )
					{
						sqlFuzzer.getNextFuzzedTestStep( testStep, param );
						runCheck( testStep, context, securityTestLog, testCaseRunner, originalResponse,
								"Possible SQL injection vulenerability detected" );
						// maybe this fuzzer can be implemented to wrap the
						// security
						// check not vice versa

					}

				}
			}
			else
			{
				Fuzzer sqlFuzzer = Fuzzer.getSQLFuzzer();

				while( sqlFuzzer.hasNext() )
				{
					sqlFuzzer.getNextFuzzedTestStep( testStep, getParamsToCheck() );
					runCheck( testStep, context, securityTestLog, testCaseRunner, originalResponse,
							"Possible SQL injection vulenerability detected" );

					// maybe this fuzzer can be implemented to wrap the
					// security
					// check not vice versa

				}

			}
		}
		//TODO
		return null;
	}

	//QUESTION:
	/*
	 * Is checking for "SQL Error" string enough for this?
	 * 
	 * What if this string is part of some tutorial?
	 */
	public SecurityCheckRequestResult analyze( TestStep testStep, SecurityTestRunContext context, SecurityTestLogModel securityTestLog,
			SecurityCheckRequestResult securityCheckResult )
	{
		// TODO: Make this test more extensive
		AbstractHttpRequest<?> lastRequest = getRequest( testStep );
		MessageExchange messageExchange = new HttpResponseMessageExchange((HttpTestRequestInterface<?>) lastRequest );

		securityCheckResult.setMessageExchange( messageExchange );
		

		if( SecurityCheckUtil.contains( context, new String( messageExchange.getRawResponseData() ),
				"SQL Error", false ) )
		{
			securityTestLog.addEntry( new SecurityTestLogMessageEntry( "SQL Error displayed in response", null
			/* new HttpResponseMessageExchange(lastRequest) */) );
			securityCheckResult.setStatus( SecurityCheckStatus.FAILED );
		}
		else
		{
			securityCheckResult.setStatus( SecurityCheckStatus.OK );
		}
		//TODO 
		return null;
	}

	@Override
	public boolean acceptsTestStep( TestStep testStep )
	{
		return testStep instanceof SamplerTestStep;
	}

	@Override
	public SecurityCheckConfigPanel getComponent()
	{
		return null;
	}

	@Override
	public String getType()
	{
		return TYPE;
	}

	@Override
	public void checkForSensitiveInformationExposure( TestStep testStep, SecurityTestRunContext context,
			SecurityTestLogModel securityTestLog )
	{
		InformationExposureCheck iec = new InformationExposureCheck( testStep, config, null, null );
		iec.analyze( testStep, context, securityTestLog, null );
	}
}