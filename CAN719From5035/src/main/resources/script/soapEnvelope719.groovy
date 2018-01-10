import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
import groovy.xml.*;



def Message processData(Message message) {
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
	
	
	// beging mapping
	
	String property = map.get("PERSON_ID_EXTERNAL");	
	def pernrs = property == null ? "": property.split(",");
	
	property = map.get("AVOID_UPSERT");
	def avoidUpsert = property = null? "": property.split(",");
	for(String s : avoidUpsert){
		if(s.length()>0){
		     message.setProperty(s.trim(), "TRUE");       
		}              
	}

	
    property = map.get("COUNTRY");
	def country = property == null ? "CA": property;
	
    property = map.get("START_DATE");
    def startDate = property == null ? "": property;

    property = map.get("END_DATE");
    def endDate = property == null ? "": property;

    property = map.get("CHANGED_ON_DATE");
    def changedOnDate = property == null ? "": property;
    

	String payload = "";
   
   	payload += "<CountryCode>"+ country +"</CountryCode>";
   	
    //pernr
   // String pernrsXML = "";
   // if(pernrs.length>0){
   //      for(def pernr in pernrs){
   //     pernrsXML += "<PersonnelNumber>" + pernr + "</PersonnelNumber>"
   // }
   // payload += "<PersonalNumber>" + pernrsXML + "</PersonalNumber>";       
   // }

    
	//end date
	if(endDate.length()>0){
	      payload += "<EndDate>" + endDate + "</EndDate>";
	}

   
    // start date
    if(startDate.length()>0){
         payload += "<BeginDate>" + startDate + "</BeginDate>";
        
    }

    // begin date
    if(changedOnDate.length()>0){
          payload += "<ChangedOnDate>" + changedOnDate + "</ChangedOnDate>";
        
    }
    
	payload = context + payload;
	
	
	message.setBody("<Request>" + payload + "</Request>");
	
	return message;
}

