package com.eviware.soapui.impl.rest.mock;

import com.eviware.soapui.impl.wsdl.mock.DispatchException;
import com.eviware.soapui.impl.wsdl.mock.WsdlMockRunContext;
import com.eviware.soapui.model.mock.MockResult;
import com.eviware.soapui.model.support.AbstractMockDispatcher;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestMockDispatcher extends AbstractMockDispatcher
{

	private RestMockService mockService;
	private WsdlMockRunContext mockContext;

	private final static Logger log = Logger.getLogger( RestMockDispatcher.class );

	public RestMockDispatcher( RestMockService mockService, WsdlMockRunContext mockContext )
	{
		this.mockService = mockService;
		this.mockContext = mockContext;
	}

	@Override
	public MockResult dispatchRequest( HttpServletRequest request, HttpServletResponse response )
			throws DispatchException
	{
		try
		{
			RestMockRequest restMockRequest = new RestMockRequest( request, response, mockContext );
			MockResult mockResult = getMockResult( restMockRequest );
			mockService.runAfterRequestScript( mockContext, mockResult );
			return mockResult;
		}
		catch( Exception e )
		{
			throw new DispatchException( e );
		}
	}

	private MockResult getMockResult( RestMockRequest restMockRequest ) throws DispatchException
	{
		RestMockAction mockAction = ( RestMockAction )mockService.findOperationMatchingPath( restMockRequest.getPath() );

		if( mockAction != null )
		{
			return mockAction.dispatchRequest( restMockRequest );
		}
		else
		{
			return getDefaultResponse( restMockRequest );
		}

	}



	private RestMockResult getDefaultResponse( RestMockRequest restMockRequest ) throws DispatchException
	{
		RestMockResult result = null;
		try
		{
			result = new RestMockResult( restMockRequest );
		}
		catch( Exception e )
		{
			throw new DispatchException( e );
		}

		return result;

	}
}
