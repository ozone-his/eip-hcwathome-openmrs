/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.hcwathome.openmrs;

import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.client.api.IGenericClient;

/**
 * An instance of this class is used to communicate with an OpenMRS server using fhir.
 */
@Component("openmrsFhirClientWrapper")
public class OpenmrsFhirClient extends BaseFhirClient {
	
	private IGenericClient openmrsClient;
	
	public OpenmrsFhirClient(IGenericClient openmrsClient) {
		super("OpenMRS");
		this.openmrsClient = openmrsClient;
	}
	
	@Override
	protected IGenericClient getFhirClient() {
		return openmrsClient;
	}
	
}
