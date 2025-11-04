/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * An instance of this class is used to communicate with the hcw@home backend server using fhir.
 */
@Slf4j
@Component
public class HcwFhirClient {
	
	private static final String SUB_PATH_API_V1 = "/api/v1/";
	
	private static final String SUB_PATH_FHIR = SUB_PATH_API_V1 + "fhir";
	
	@Value("${hcwathome.backend.url}")
	private String baseUrl;
	
	@Value("${hcwathome.user.email}")
	private String email;
	
	@Value("${hcwathome.password}")
	private char[] password;
	
	private FhirContext fhirContext;
	
	private IGenericClient fhirClient;
	
	private IGenericClient getFhirClient() {
		if (fhirClient == null) {
			synchronized (this) {
				if (fhirClient == null) {
					fhirContext = FhirContext.forR4();
					fhirContext.getRestfulClientFactory().setConnectTimeout(30000);
					fhirContext.getRestfulClientFactory().setConnectionRequestTimeout(120000);
					fhirContext.getRestfulClientFactory().setSocketTimeout(120000);
					fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
					fhirClient = fhirContext.newRestfulGenericClient(baseUrl + SUB_PATH_FHIR);
					fhirClient.registerInterceptor(new AuthInterceptor(baseUrl + SUB_PATH_API_V1, email, password));
				}
			}
		}
		
		return fhirClient;
	}
	
	/**
	 * Fetches an invite from hcw@home with an identifier matching the specified openmrs appointment
	 * uuid.
	 * 
	 * @param uuid the openmrs appointment uuid to match
	 * @return a fhir Appointment if a match is found otherwise null
	 */
	public Appointment getAppointmentByIdentifier(String uuid) {
		if (log.isDebugEnabled()) {
			log.debug("Getting appointment from hcw@home with identifier: {}", uuid);
		}
		
		try {
			Bundle bundle = (Bundle) getFhirClient().search().forResource(Appointment.class)
			        .where(Appointment.IDENTIFIER.exactly().identifier(uuid)).execute();
			if (bundle.getEntry().size() == 1) {
				if (log.isDebugEnabled()) {
					log.debug("Found appointment in hcw@home with identifier: {}", uuid);
				}
				
				return (Appointment) bundle.getEntry().get(0).getResource();
			} else if (bundle.getEntry().size() > 1) {
				throw new EIPException("Found multiple appointments in hcw@home with external identifier " + uuid);
			}
		}
		catch (ResourceNotFoundException e) {
			//Ignore
		}
		
		if (log.isDebugEnabled()) {
			log.debug("No appointment found in hcw@home with identifier: {}", uuid);
		}
		
		return null;
	}
	
	/**
	 * Creates an invite in hcw@home for the specified appointment.
	 * 
	 * @param appointment the appointment to create
	 */
	public void createAppointment(Appointment appointment) {
		if (log.isDebugEnabled()) {
			log.debug("Creating appointment in hcw@home");
		}
		
		MethodOutcome outcome;
		try {
			outcome = getFhirClient().create().resource(appointment).execute();
		}
		catch (Exception e) {
			throw new EIPException(getErrorMessage(e, "create"));
		}
		
		if (!outcome.getCreated()) {
			throw new EIPException("Unexpected outcome " + outcome + " when creating invite in hcw@home");
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Successfully created appointment in hcw@home");
		}
	}
	
	/**
	 * Updates an invite in hcw@home matching the specified appointment.
	 * 
	 * @param appointment the appointment to update
	 */
	public void updateAppointment(Appointment appointment) {
		if (log.isDebugEnabled()) {
			log.debug("Updating appointment in hcw@home");
		}
		
		MethodOutcome outcome;
		try {
			outcome = getFhirClient().update().resource(appointment).execute();
		}
		catch (Exception e) {
			throw new EIPException(getErrorMessage(e, "update"));
		}
		
		int statusCode = outcome.getResponseStatusCode();
		if (statusCode != 200) {
			throw new EIPException("Failed to update invite in hcw@home, status code " + statusCode);
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Successfully updated appointment in hcw@home");
		}
	}
	
	/**
	 * Deletes an invite from hcw@home matching the specified appointment.
	 *
	 * @param appointment the appointment to delete
	 */
	public void deleteAppointment(Appointment appointment) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting appointment from hcw@home");
		}
		
		MethodOutcome outcome;
		try {
			outcome = getFhirClient().delete().resource(appointment).execute();
		}
		catch (Exception e) {
			throw new EIPException(getErrorMessage(e, "delete"));
		}
		
		int statusCode = outcome.getResponseStatusCode();
		if (statusCode != 200) {
			throw new EIPException("Failed to delete invite from hcw@home, status code " + statusCode);
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Successfully deleted appointment from hcw@home");
		}
	}
	
	/**
	 * Fetches the consultation from hcw@home associated to the invite matching the specified openmrs
	 * appointment uuid.
	 *
	 * @param appointmentUuid the openmrs appointment uuid to match
	 * @return a fhir Encounter if a match is found otherwise null
	 */
	public Encounter getEncounterByAppointment(String appointmentUuid) {
		if (log.isDebugEnabled()) {
			log.debug("Getting encounter from hcw@home for appointment with identifier: {}", appointmentUuid);
		}
		
		try {
			Bundle bundle = (Bundle) getFhirClient().search().forResource(Encounter.class).where(
			    Encounter.APPOINTMENT.hasChainedProperty(Appointment.IDENTIFIER.exactly().identifier(appointmentUuid)))
			        .execute();
			if (bundle.getEntry().size() == 1) {
				if (log.isDebugEnabled()) {
					log.debug("Getting encounter from hcw@home for appointment with identifier: {}", appointmentUuid);
				}
				
				return (Encounter) bundle.getEntry().get(0).getResource();
			} else if (bundle.getEntry().size() > 1) {
				throw new EIPException("Found multiple encounters from hcw@home for appointment with external identifier "
				        + appointmentUuid);
			}
		}
		catch (ResourceNotFoundException e) {
			//Ignore
		}
		
		if (log.isDebugEnabled()) {
			log.debug("No encounter found in hcw@home for appointment with identifier: {}", appointmentUuid);
		}
		
		return null;
	}
	
	private String getErrorMessage(Exception e, String operation) {
		String msg = getServerErrorMessage(e);
		if (StringUtils.isBlank(msg)) {
			msg = "Failed to " + operation + " invite in hcw@home";
		}
		
		return msg;
	}
	
	protected String getServerErrorMessage(Exception e) {
		if (e instanceof BaseServerResponseException) {
			return ((BaseServerResponseException) e).getResponseBody();
		}
		
		return null;
	}
	
}
