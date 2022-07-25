package cc.ecisr.vocnyan.struct;

public class Configs {
	public static final String SEARCH_SEPARATE_SIGN = "$";
	public static final String SEARCH_SEPARATE_SIGN_REGEX = "\\$";
	public static final String PARSE_UPPER_SEPARATE_SIGN = "//";
	public static SORT_RULE sortRule = SORT_RULE.LENGTH;
	
	public enum SORT_RULE {
		LENGTH, UNICODE, ID, ID_REV, NO
	}
}
