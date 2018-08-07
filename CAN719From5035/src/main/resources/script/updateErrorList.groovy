import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;



def Message processData(Message message) {
def messageLog = messageLogFactory.getMessageLog(message);
	def pmap = message.getProperties();
	String enableLogging = pmap.get("ENABLE_LOGGING");
	String pernrs = pmap.get("PERSON_ID_EXTERNAL_PARAMETER");
	String msg = "";
	String code = "";
	def currentPernr = pmap.get("EE_CURRENT") as String;
	List notifyList =pmap.get("NOTIFY_LIST");
	
	//Body
	String inXML = message.getBody(java.lang.String) as String;
	def body = new XmlSlurper().parseText(inXML);
	
	def results = null;
	List<String> errorList = new ArrayList<String>();
	
	if(inXML.indexOf("batchChangeSetPartResponse")>=0){
	    results = body.batchChangeSetResponse.batchChangeSetPartResponse.statusInfo as String;
	}
	
	if(results.indexOf("Error")>=0){
		for(item in notifyList){
            if(item.personnelNumber.equals(currentPernr)){
                  item.status = "4";                            
              }
        }
	}else if(results.indexOf("OK")>=0){
		for(item in notifyList){
            if(item.personnelNumber.equals(currentPernr)){
                  item.status = "0";                            
              }
        }
	}
	
	messageLog.setStringProperty("upsert status: ", results);

	return message;
}