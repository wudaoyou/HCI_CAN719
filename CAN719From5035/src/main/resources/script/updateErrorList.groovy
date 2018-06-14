import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
def void updateNotifyList(List errorList, Message message){
def pmap = message.getProperties();
		List notifyList =pmap.get("NOTIFY_LIST");
		def eeCount = pamp.get("EE_COUNT");
		
        if(errorList.size()>0){
            for(e in errorList){
            	String s = e;
                 int pos = s.indexOf("startDate");
		 		 s=s.substring(pos+10);
		 		 String startDate = s.substring(0, s.indexOf('T'));
		         startDate += "T00:00:00Z";
		         pos = s.indexOf("userId");
				 s=s.substring(pos+ 7);
				 String userId = s.substring(0, s.indexOf('}'));
		         
		         for(item in notifyList){
		             if(item.userId.equals(userId)&& item.beginDate.equals(startDate)&& !(item.status.equals("4"))){
		                   item.status = "4";                            
		               }
		         }
                      
           }
        }  
		message.setProperty("EE_COUNT",eeCount-errorList.size());
		message.setProperty("NOTIFY_LIST",notifyList);
 }

def Message processData(Message message) {
def messageLog = messageLogFactory.getMessageLog(message);
	def pmap = message.getProperties();
	String enableLogging = pmap.get("ENABLE_LOGGING");
	String pernrs = pmap.get("PERSON_ID_EXTERNAL_PARAMETER");
	
	//Body
	String inXML = message.getBody(java.lang.String) as String;
	def body = new XmlSlurper().parseText(inXML);
	
	def results = null;
	List<String> errorList = new ArrayList<String>();
	
	if(inXML.indexOf("EmpJobUpsertResponse")>=0){
	    results = body.EmpJobUpsertResponse;
	}else if (inXML.indexOf("EmpPayCompRecurringUpsertResponse")>=0){
	          results = body.EmpPayCompRecurringUpsertResponse;
	}else if (inXML.indexOf("EmpCompensationUpsertResponse")>=0){
	          results = body.EmpCompensationUpsertResponse;
	 }

  	if(results != null){
  	    results.each{
         def status = it.status
         messageLog.setStringProperty("message : ", it.message.toString());
         if(it.status.equals("ERROR")){
               errorList.add(it.message.toString());
         }  
    }
                 
  }
	
	if(enableLogging != null && enableLogging.toUpperCase().equals("TRUE")){
		
		if(messageLog != null){
			messageLog.addAttachmentAsString("EC update result ", inXML, "text/xml");
		}
	}
	
	updateNotifyList(errorList, message);
	return message;
}