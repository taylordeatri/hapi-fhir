package ca.uhn.fhir.server.exceptions;

public class ResourceNotFoundException extends AbstractResponseException {

	public ResourceNotFoundException(long theId) {
		super(404, "Resource " + theId + " is not known");
	}

	private static final long serialVersionUID = 1L;

}