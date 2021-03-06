import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
import groovy.xml.*;

def Message processData(Message message) {

	def pmap = message.getProperties();
	String enableLogging = pmap.get("ENABLE_LOGGING");
	String pernrs = pmap.get("PERSON_ID_EXTERNAL_PARAMETER");
	def body = message.getBody(java.lang.String) as String;
	String payload = "";
	// form XML for empjob
	HashMap xmlMap = pmap.get("XML_LIST");
	
	int eeCount = 0;
	
		//get keys (employee external) from the xml map
	List<String> keys = new ArrayList<>(xmlMap.keySet());
	def errorEEs = pmap.get("NOTIFY_LIST");
	
	if(errorEEs.size()>0){
	    for(def item: errorEEs){
	    	if(item.status.equals("4")){
    			keys.remove(item.personnelNumber);
			}
	    }
	}
	
	for (String key : keys) {
		 item = xmlMap.get(key);
		 if (item.payRecurPremXML.length()>0){  
		 		eeCount++;
		        payload += item.payRecurPremXML;
		          }
	}
	
	if(enableLogging != null && enableLogging.toUpperCase().equals("TRUE")){
		
		def messageLog = messageLogFactory.getMessageLog(message);
		if(messageLog != null){
			messageLog.addAttachmentAsString("07 upsert to premium ", payload, "text/xml");
		}
	}
	
	message.setProperty("EE_COUNT", eeCount);
	
	message.setBody("<EmpPayCompRecurring>" + payload + "</EmpPayCompRecurring>");   
	return message;
}