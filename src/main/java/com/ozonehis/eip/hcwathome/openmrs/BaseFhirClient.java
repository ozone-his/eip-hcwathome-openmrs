/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.DomainResource;
import org.openmrs.eip.EIPException;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for classes that interact with a FHIR server.
 */
@Slf4j
public abstract class BaseFhirClient {
	
	private String serverName;
	
	public BaseFhirClient(String serverName) {
		this.serverName = serverName;
	}
	
	/**
	 * Creates the specified resource in the fhir server.
	 *
	 * @param resource the resource to create
	 */
	public void create(DomainResource resource) {
		if (log.isDebugEnabled()) {
			log.debug("Creating {} in {}", resource.fhirType(), serverName);
		}
		
		MethodOutcome outcome;
		try {
			outcome = getFhirClient().create().resource(resource).execute();
		}
		catch (Exception e) {
			throw new EIPException(getErrorMessage(e, "create"));
		}
		
		if (!outcome.getCreated()) {
			throw new EIPException("Unexpected outcome " + outcome + " when creating invite in " + serverName);
		}
		
		if (log.isDebugEnabled()) {
			log.debug("Successfully created {} in {}", resource.fhirType(), serverName);
		}
	}
	
	protected String getErrorMessage(Exception e, String operation) {
		String msg = getServerErrorMessage(e);
		if (StringUtils.isBlank(msg)) {
			msg = "Failed to " + operation + " invite in " + serverName;
		}
		
		return msg;
	}
	
	private String getServerErrorMessage(Exception e) {
		if (e instanceof BaseServerResponseException) {
			return ((BaseServerResponseException) e).getResponseBody();
		}
		
		return null;
	}
	
	/**
	 * Gets the {@link IGenericClient} instance
	 *
	 * @return IGenericClient
	 */
	protected abstract IGenericClient getFhirClient();
	
}
