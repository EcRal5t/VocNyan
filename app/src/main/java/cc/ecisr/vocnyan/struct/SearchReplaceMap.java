package cc.ecisr.vocnyan.struct;

import java.util.ArrayList;

public class SearchReplaceMap {
	static public ArrayList<String> replaceMapKey = new ArrayList<>();
	static public ArrayList<String> replaceMapVal = new ArrayList<>();
	static public ArrayList<String> ellipsisMapKey = new ArrayList<>();
	static public ArrayList<String> ellipsisMapVal = new ArrayList<>();
	static public ArrayList<String> displayMapKey = new ArrayList<>();
	static public ArrayList<String> displayMapVal = new ArrayList<>();
	
	static public void initialSearchReplaceMap() {
		addReplaceMap("q(?=[aeoiujwɴ])","Ɂ");
		addReplaceMap("n(?=[^aeoiu]|$)","ɴ");
		addReplaceMap("q","\uA7AF");
		addReplaceMap("sh","š");
		addReplaceMap("zh","ž");
		addReplaceMap("ch","č");
		addReplaceMap("hu","ɸu");
		addReplaceMap("hw","ɸ");
		addReplaceMap("hi","çi");
		addReplaceMap("hj","ç");
		addReplaceMap("N","ñ");
		
		addEllipsisMap("(?<=[aeoiu]|^)(?=[aeoiujwɴ])", "[Ɂ']?");
		addEllipsisMap("s", "[sš]");
		addEllipsisMap("z", "[zž]");
		addEllipsisMap("c", "[cč]");
		
		addDisplayMap("ꞯ", "Q");
		addDisplayMap("(?=[2-9]\\.)", "\n");
		addDisplayMap("\n\n", "\n");
		addDisplayMap("\n\n", "\n");
	}
	
	static void addReplaceMap(String key, String val) {
		replaceMapKey.add(key);
		replaceMapVal.add(val);
	}
	static void addEllipsisMap(String key, String val) {
		ellipsisMapKey.add(key);
		ellipsisMapVal.add(val);
	}
	static void addDisplayMap(String key, String val) {
		displayMapKey.add(key);
		displayMapVal.add(val);
	}
	
}
