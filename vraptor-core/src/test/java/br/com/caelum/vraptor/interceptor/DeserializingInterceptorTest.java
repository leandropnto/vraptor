package br.com.caelum.vraptor.interceptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import br.com.caelum.vraptor.Consumes;
import br.com.caelum.vraptor.core.InterceptorStack;
import br.com.caelum.vraptor.deserialization.Deserializers;
import br.com.caelum.vraptor.resource.DefaultResourceMethod;
import br.com.caelum.vraptor.resource.ResourceMethod;
import br.com.caelum.vraptor.test.VRaptorMockery;
import br.com.caelum.vraptor.view.HttpResult;


public class DeserializingInterceptorTest {
	private VRaptorMockery mockery;
	private DeserializingInterceptor interceptor;
	private DefaultResourceMethod consumeXml;
	private DefaultResourceMethod doesntConsume;
	private HttpServletRequest request;
	private HttpResult result;
	private InterceptorStack stack;
	private Deserializers deserializers;

	@Before
	public void setUp() throws Exception {
		mockery = new VRaptorMockery();

		stack = mockery.mock(InterceptorStack.class);
		request = mockery.mock(HttpServletRequest.class);
		result = mockery.mock(HttpResult.class);
		deserializers = mockery.mock(Deserializers.class);
		interceptor = new DeserializingInterceptor(request, result, deserializers);
		consumeXml = new DefaultResourceMethod(null, DummyResource.class.getDeclaredMethod("consumeXml"));
		doesntConsume = new DefaultResourceMethod(null, DummyResource.class.getDeclaredMethod("doesntConsume"));
	}


	@After
	public void tearDown() throws Exception {
		mockery.assertIsSatisfied();
	}

	static class DummyResource {
		@Consumes("application/xml")
		public void consumeXml() {}

		public void doesntConsume() {}
	}
	@Test
	public void shouldOnlyAcceptMethodsWithConsumesAnnotation() throws Exception {
		assertTrue(interceptor.accepts(consumeXml));
		assertFalse(interceptor.accepts(doesntConsume));
	}

	@Test
	public void willSetHttpStatusCode415IfTheResourceMethodDoesNotSupportTheGivenMediaTypes() throws Exception {
		mockery.checking(new Expectations(){{
			allowing(request).getContentType();
			will(returnValue("image/jpeg"));

			one(result).sendError(with(equal(415)), with(any(String.class)));
			never(stack).next(with(any(ResourceMethod.class)), with(any(Object.class)));
		}});

		interceptor.intercept(stack, consumeXml, null);
	}

	@Test
	public void willCallDeserializeWithRequestInformation() throws Exception {
		mockery.checking(new Expectations() {{
			allowing(request).getInputStream();

			allowing(request).getContentType();
			will(returnValue("application/xml"));

			one(deserializers).deserialize(null, "application/xml", consumeXml);
		}});

		interceptor.intercept(stack, consumeXml, null);
	}

}
