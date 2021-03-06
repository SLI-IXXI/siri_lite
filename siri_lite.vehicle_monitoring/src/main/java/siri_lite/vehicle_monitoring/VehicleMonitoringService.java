package siri_lite.vehicle_monitoring;

import java.util.NoSuchElementException;

import javax.ejb.Stateless;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import lombok.extern.log4j.Log4j;
import siri_lite.common.Color;
import siri_lite.common.Configuration;
import siri_lite.common.DefaultAsyncHandler;
import siri_lite.common.DefaultParameters;
import siri_lite.common.RequestStructureFactory;
import siri_lite.common.SiriProducerDocServices;
import siri_lite.common.SiriProducerDocServicesFactory;
import siri_lite.common.SiriStructureFactory;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.ObjectFactory;
import uk.org.siri.siri.ParticipantRefStructure;
import uk.org.siri.siri.ProducerResponseEndpointStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleMonitoringRequestStructure;
import uk.org.siri.wsdl.VehicleMonitoringAnswerStructure;
import uk.org.siri.wsdl.WsServiceRequestInfoStructure;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

@Log4j
@Stateless
public class VehicleMonitoringService {

	private static ObjectFactory factory = new ObjectFactory();
	private static uk.org.siri.wsdl.ObjectFactory wsFactory = new uk.org.siri.wsdl.ObjectFactory();

	public void getVehicleMonitoring(MultivaluedMap<String, String> properties, AsyncResponse response) {

		Monitor monitor = MonitorFactory.start("VehicleMonitoringService.getVehicleMonitoring()");

		SiriProducerDocServices service = null;
		try {
			Configuration configuration = Configuration.getInstance();

			// validate parameters
			VehicleMonitoringParameters parameters = new VehicleMonitoringParameters();
			parameters.configure(properties);
			parameters.validate();

			// create message
			WsServiceRequestInfoStructure requestInfo = SiriStructureFactory
					.createWsServiceRequestInfoStructure(configuration);

			requestInfo.setRequestTimestamp(parameters.getRequestTimestamp());
			requestInfo.setMessageIdentifier(parameters.getMessageIdentifier());

			// RequestorRef
			ParticipantRefStructure requestorRef = SiriStructureFactory.createParticipantRef(parameters.getRequestorRef(),
					null);
			requestInfo.setRequestorRef(requestorRef);

			// DelegatorRef
			if (configuration.getDelegatorRef() != null && !configuration.getDelegatorRef().isEmpty()) {
				ParticipantRefStructure delegatorRef = SiriStructureFactory.createParticipantRef(configuration
						.getDelegatorRef(), null);
				requestInfo.setDelegatorRef(delegatorRef);
			}

			// AccountId
			requestInfo.setAccountId(parameters.getAccountId());

			// AccountKey
			requestInfo.setAccountKey(parameters.getAccountKey());

			VehicleMonitoringRequestStructure request = RequestStructureFactory.create(
					VehicleMonitoringRequestStructure.class, configuration, parameters);
			ExtensionsStructure requestExtension = factory.createExtensionsStructure();

			uk.org.siri.wsdl.VehicleMonitoringRequestStructure wsRequest = wsFactory
					.createVehicleMonitoringRequestStructure();
			wsRequest.setServiceRequestInfo(requestInfo);
			wsRequest.setRequest(request);
			wsRequest.setRequestExtension(requestExtension);

			JAXBElement<uk.org.siri.wsdl.VehicleMonitoringRequestStructure> jaxbElement = new JAXBElement<uk.org.siri.wsdl.VehicleMonitoringRequestStructure>(
					new QName("http://wsdl.siri.org.uk", "GetVehicleMonitoring"),
					uk.org.siri.wsdl.VehicleMonitoringRequestStructure.class, wsRequest);

			// invoke web service
			service = SiriProducerDocServicesFactory.make();
			VehicleMonitoringHandler handler = new VehicleMonitoringHandler(configuration, parameters, response);
			handler.setService(service);
			service.invoke(jaxbElement, handler, parameters);
		} catch (NoSuchElementException e) {
			log.error(e.getMessage(), e);
			Response payload = Response.status(Status.SERVICE_UNAVAILABLE).build();
			response.resume(payload);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Response payload = Response.status(Status.BAD_REQUEST).build();
			response.resume(payload);
			if (service != null) {
				SiriProducerDocServicesFactory.passivate(service);
				service = null;
			}
		}

		log.info(Color.GREEN + "[DSU] " + monitor.stop() + Color.NORMAL);
	}

	class VehicleMonitoringHandler extends DefaultAsyncHandler<VehicleMonitoringAnswerStructure> {

		public VehicleMonitoringHandler(Configuration configuration, DefaultParameters parameters, AsyncResponse response)
				throws JAXBException {
			super(configuration, parameters, response);
		}

		@Override
		public void handleResponse(VehicleMonitoringAnswerStructure response) {

			// initialize siri stucture
			Siri siri = factory.createSiri();
			ServiceDelivery delivery = factory.createServiceDelivery();
			ProducerResponseEndpointStructure responseInfo = response.getServiceDeliveryInfo();
			delivery.setResponseTimestamp(responseInfo.getResponseTimestamp());
			delivery.setProducerRef(responseInfo.getProducerRef());
			delivery.setAddress(responseInfo.getAddress());
			delivery.setResponseMessageIdentifier(responseInfo.getResponseMessageIdentifier());
			delivery.setRequestMessageRef(responseInfo.getRequestMessageRef());
			delivery.getVehicleMonitoringDelivery().addAll(response.getAnswer().getVehicleMonitoringDelivery());
			siri.setServiceDelivery(delivery);

			// resume
			resume(siri, configuration.getVehicleMonitoringMaxAge());
		}
	}

}