package siri_lite.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.xml.ws.Holder;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

import lombok.extern.log4j.Log4j;

import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import siri_lite.common.Color;
import siri_lite.common.SiriStructureFactory;
import siri_lite.discovery.StopPointsDiscoveryParameters;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.StopPointsDeliveryStructure;
import uk.org.siri.siri.StopPointsDiscoveryRequestStructure;
import uk.org.siri.wsdl.StopPointsDiscoveryError;

@Log4j
public class SoapFaultTest extends Arquillian {

	@Deployment(testable = false)
	public static EnterpriseArchive createDeployment() {
		final EnterpriseArchive result = ShrinkWrap.createFromZipFile(
				EnterpriseArchive.class, new File(
						"../siri_lite.server/target/siri_lite.ear"));
		return result;
	}

	private Server service = null;

	public void initialize() throws Exception {
		service = new Service();
		service.initialize();
	}

	public void dispose() throws Exception {
		service.dispose();
	}

	@Test
	@RunAsClient
	public void test() throws Exception {

		log.info(Color.YELLOW + "[DSU] execute test : "
				+ this.getClass().getSimpleName() + Color.NORMAL);
		try {
			initialize();
			// invoke service
			String URL = "http://localhost:8080/siri/2.0.0/stoppoints-discovery.xml";
			List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();
			parameters
					.add(new BasicNameValuePair(
							StopPointsDiscoveryParameters.REQUESTOR_REF,
							"REQUESTORREF"));

			String url = Utils.buildURL(URL, parameters);
			Client client = ClientBuilder.newClient();
			client.register(LoginFilter.class);
			WebTarget target = client.target(url);
			Response response = target.request().get();
			response.close();

			// response test
			Assert.assertEquals(response.getStatus(), 500);
		} finally {
			dispose();
		}
	}

	@WebService(name = "SiriWS", targetNamespace = "http://wsdl.siri.org.uk")
	public class Service extends Server {

		public Service() {
			super();
		}

		@WebMethod(operationName = "StopPointsDiscovery", action = "StopPointsDiscovery")
		@RequestWrapper(localName = "StopPointsDiscovery", targetNamespace = "http://wsdl.siri.org.uk", className = "uk.org.siri.wsdl.WsStopPointsDiscoveryStructure")
		@ResponseWrapper(localName = "StopPointsDiscoveryResponse", targetNamespace = "http://wsdl.siri.org.uk", className = "uk.org.siri.wsdl.WsStopPointsDiscoveryAnswerStructure")
		public void stopPointsDiscovery(
				@WebParam(name = "Request", targetNamespace = "") StopPointsDiscoveryRequestStructure request,
				@WebParam(name = "RequestExtension", targetNamespace = "") ExtensionsStructure requestExtension,
				@WebParam(name = "Answer", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<StopPointsDeliveryStructure> answer,
				@WebParam(name = "AnswerExtension", targetNamespace = "", mode = WebParam.Mode.OUT) Holder<ExtensionsStructure> answerExtension)
				throws StopPointsDiscoveryError {

			throw new StopPointsDiscoveryError(
					"SERVICE_NOT_AVAILABLE",
					SiriStructureFactory
							.createOtherErrorConditionStructure("SoapFaultTest"));

		}
	}

}
