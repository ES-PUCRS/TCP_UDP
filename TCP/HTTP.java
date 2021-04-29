public enum HTTP {

	OK("{200: OK}"),
	CREATED("{201: CREATED}"),
	NO_CONTENT("{204 NO CONTENT}"),
	
	FOUND("{302: FOUND}"),

	NOT_FOUND("{404: NOT FOUND}"),
	BAD_REQUEST("{400: BAD REQUEST}"),
	UNAUTHORIZED("{401: UNAUTHORIZED}"),
	METHOD_NOT_ALLOWED("{405: METHOD NOT ALLOWED}"),
	TIME_OUT("{408: REQUEST TIME OUT}"),
	TEA_POT("{418: I'm a teapot}");

	public String description;
	HTTP (String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}
}