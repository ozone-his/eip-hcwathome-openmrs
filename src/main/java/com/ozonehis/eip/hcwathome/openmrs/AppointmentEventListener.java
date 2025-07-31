package com.ozonehis.eip.hcwathome.openmrs;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppointmentEventListener extends RouteBuilder {

    @Override
    public void configure() {
        errorHandler("watcherErrorHandler");

        from("direct:appointment-event-listener")
                .routeId("appointment-event-listener")
                .log("Received appointment event with body : ${body}")
                .log("Received appointment event: ${exchangeProperty.event}");
    }

}
