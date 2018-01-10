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
	
	for (String key : xmlMap.keySet()) {
		 item = xmlMap.get(key);
   		payload += item.compXML;
	}
	
	if(enableLogging != null && enableLogging.toUpperCase().equals("TRUE")){
		
		def messageLog = messageLogFactory.getMessageLog(message);
		if(messageLog != null){
			messageLog.addAttachmentAsString("05 upsert to comp ", payload, "text/xml");
		}
	}
	message.setBody("<EmpCompensation>" + payload + "</EmpCompensation>");   
	return message;
}