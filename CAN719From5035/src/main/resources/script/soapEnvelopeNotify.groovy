import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
import groovy.xml.*;


def Message processData(Message message) {

def messageLog = messageLogFactory.getMessageLog(message);
	TimeZone.setDefault(TimeZone.getTimeZone('UTC'));
	def now = new Date();
    String nowStr = now.format("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    String uniqueID = UUID.randomUUID().toString();
    
    String context = "<Context><MessageName>CAN719</MessageName><Timestamp>"+ nowStr+"</Timestamp><SendingApplication>HCI</SendingApplication><TrackingId>"+uniqueID+"</TrackingId><UserId>EC_ECC_INSVC</UserId><ApplicationName>EC</ApplicationName></Context>";
	 
	//Body
	//String inXML = message.getBody(java.lang.String) as String;
	def body = "";
	
	//Headers
	def map = message.getProperties();
	
	List notifyList =map.get("NOTIFY_LIST");
	
	int eeCount = 0;

	// beging mapping
	
	String payload = "";
	// get payload
	for(item in notifyList){
		payload+="<NotifyEmployee>";
		payload+="<PersonnelNumber>"+item.personnelNumber+"</PersonnelNumber>";
		payload+="<EndDate>"+item.endDate+"</EndDate>";
		payload+="<BeginDate>"+item.beginDate+"</BeginDate>";
		payload+="<ChangedOnDate>"+item.changedOnDate+"</ChangedOnDate>";
		payload+="<SequenceNumber>1</SequenceNumber>"; //this is a dummy value. sequence number is not used for this op.
		payload+="<Status>"+item.status+"</Status>";  // status 99 = reset, 0 = success, 1 = processing, 4 = failed 
		payload+="</NotifyEmployee>";            
	}

	payload = context + payload;
	
	message.setBody("<Request>" + payload + "</Request>");
	return message;
}

