package fr.upem.net.chatos.clientSide;

public enum OPCodes {
	SIGN_IN(0,"pseudo"),
	SIGN_OUT(1,""), 
	REQUEST_PRIVATE(2,"pseudo"), 
	PUBLIC_MESSAGE(4,"message"),
	RECEIVE_MESSAGE(5,"sender:message"),
	DENY_PRIVATE(6,"pseudo"), 
	ACCEPT_PRIVATE(7,"pseudo"), 
	SEND_PRIVATE(8,"pseudo:address"), 
	RECEIVE_PRIVATE(9,"sender:message"),
	AUTH_ACCEPTED(100,""),
	AUTH_REFUSED(101,""),
	ERROR(104,"errormessage");

	private OPCodes(int code, String format) {
		this.code = code;
		this.format = format;
	}

	private final int code;
	private final String format;

	public int getCode() {
		return code;
	}
	
	public String getFormat() {
		return format;
	}
	
	public static String getFormatForCode(int cd) {
		for(OPCodes code : OPCodes.values()) {
			if(code.getCode() == cd) {
				return code.format;
			}
		}
		return "";
	}
	
	public static boolean hasOPCode(int cd) {
		for(OPCodes code : OPCodes.values()) {
			if(code.getCode() == cd) {
				return true;
			}
		}
		return false;
	}
}
