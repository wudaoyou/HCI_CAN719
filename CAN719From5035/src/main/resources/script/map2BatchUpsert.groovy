import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
import groovy.xml.*;

def Message processData(Message message) {

	def pmap = message.getProperties();
	String enableLogging = pmap.get("ENABLE_LOGGING");
	String pernrs = pmap.get("PERSON_ID_EXTERNAL_PARAMETER");
	def body = message.getBody(java.lang.String) as String;
	String payload = "";
	def count = pmap.get("EE_PROCESSED") as Integer;
	List eeList = pmap.get("EE_LIST");
    String currentPernr = "";
	if(eeList.size>0){
        currentPernr = eeList.remove(0);

        HashMap xmlMap = pmap.get("XML_LIST");
        item  = xmlMap.get(currentPernr);
        
        payload = "<batchChangeSetPart><method>UPSERT</method><EmpJob>" + item.jobXML + "</EmpJob></batchChangeSetPart>";

        payload = "<batchChangeSetPart><method>UPSERT</method><EmpCompensation>" + item.compXML + "</EmpCompensation></batchChangeSetPart>";

        payload = "<batchChangeSetPart><method>UPSERT</method><EmpPayCompRecurring>" +item.payRecurBasicXML + "</EmpPayCompRecurring></batchChangeSetPart>"

        payload = "<batchChangeSetPart><method>UPSERT</method><EmpPayCompRecurring>" +item.payRecurPremXML + "</EmpPayCompRecurring></batchChangeSetPart>"

        count++;

	}

    message.setProperty("EE_PROCESSED", count);
	
	message.setProperty("EE_LIST", pernrList);
	
	if(enableLogging != null && enableLogging.toUpperCase().equals("TRUE")){
		
		def messageLog = messageLogFactory.getMessageLog(message);
		if(messageLog != null){
			messageLog.addAttachmentAsString(count+" batch upsert ", payload, "text/xml");
		}
	}
	message.setBody("<EmpJob>" + payload + "</EmpJob>");   
	
	
	return message;
}