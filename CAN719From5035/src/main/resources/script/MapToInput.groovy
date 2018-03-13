/*
 * The integration developer needs to create the method processData 
 * This method takes Message object of package com.sap.gateway.ip.core.customdev.util
 * which includes helper methods useful for the content developer:
 * 
 * The methods available are:
    public java.lang.Object getBody()
    
    //This method helps User to retrieve message body as specific type ( InputStream , String , byte[] ) - e.g. message.getBody(java.io.InputStream)
    public java.lang.Object getBody(java.lang.String fullyQualifiedClassName)

    public void setBody(java.lang.Object exchangeBody)

    public java.util.Map<java.lang.String,java.lang.Object> getHeaders()

    public void setHeaders(java.util.Map<java.lang.String,java.lang.Object> exchangeHeaders)

    public void setHeader(java.lang.String name, java.lang.Object value)

    public java.util.Map<java.lang.String,java.lang.Object> getProperties()

    public void setProperties(java.util.Map<java.lang.String,java.lang.Object> exchangeProperties) 

	public void setProperty(java.lang.String name, java.lang.Object value)
 * 
 */
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
import groovy.xml.*;

public class JobData {
	String user_id;
	String custom_string9;
	String custom_string22;
	String start_date;
	String end_date;
	public String toString(){
	  return "UserID: "+ this.user_id+" custom_string9: "+this.custom_string9+ " custom_string22: "+ this.custom_string22;         
	           
	}

} 

 class UpsertXML{
    String jobXML="";
    String compXML="";
    String payRecurBasicXML="";
    String payRecurPremXML="";
}

class NotifyItem {
    String personnelNumber;
    String userId;
	String endDate;
	String beginDate;
	String changedOnDate;
	String sequenceNumber = "0";//dummy
	String status="";
	String toString(){
	    return personnelNumber+" "+userId +" "+ endDate+" "+ beginDate+" "+ endDate+" "+ status;
	}

}


def Message processData(Message message) {
	
	//Body
	String inXML = message.getBody(java.lang.String) as String;
	def body = new XmlSlurper().parseText(inXML);
	
	//Headers
	def pmap = message.getProperties();
	
	// Logs
	def messageLog = messageLogFactory.getMessageLog(message);
	
	HashMap<String, JobData> jobMap = new HashMap<String, JobData>();
	
	String enableLogging = pmap.get("ENABLE_LOGGING");	
	if(enableLogging != null && enableLogging.toUpperCase().equals("TRUE")){				
		if(messageLog != null){
			messageLog.addAttachmentAsString("03 From EC", inXML, "text/xml");
		}
	}
	Map<String,String> sequenceMap = new HashMap<>();

	// beging mapping
	def compoundEEs = body.CompoundEmployee;

	String payload = "";
	String userList="";
	String pernr = "";
	String uid  = "";
    List<String> userIdList = new ArrayList<>();
    List<String> pernrList = new ArrayList<>();
    String key = "";
	
	compoundEEs.each {  
		 pernr = it.person.person_id_external;
		 pernrList.add(pernr);
		 uid  = it.person.logon_user_id;
		 userIdList.add(uid);
		 	 
		for (def emp in it.person.employment_information) {
			
			for (def job in emp.job_information) {
				String end_date = job.end_date;
				String seqno = job.seq_number;
				String empStatus = job.emplStatus;
				key =pernr+"J"+job.start_date;
				sequenceMap.put(key,job.seq_number);
				// check the start date and sequence number to make sure get the correct job_information
				if (end_date.equals("9999-12-31") ) {
					// job_information found
					JobData j = new JobData();
					j.user_id = uid;
					j.custom_string9 = job.location;
					j.custom_string22 = job.custom_string22.isEmpty()? job.location: job.custom_string22;
					j.start_date = job.start_date;
					j.end_date = job.end_date;
					jobMap.put(pernr, j);
				}
			}// end of emp.job_information loop
			
			for (def comp in emp.compensation_information){
			         key = pernr+"C"+comp.start_date;     
			         sequenceMap.put(key,comp.seq_number);    
			         
			    for(def pay in emp.compensation_information.paycompensation_recurring){
             		 key = pernr+"P"+pay.pay_component+pay.start_date;         
			         sequenceMap.put(key,pay.seq_number.toString());
					 }
              
			              
			 }

			
		}// end of employment information loop
	}// end of compoundEE loop
	
	Collections.sort(pernrList);
	Collections.sort(userIdList);
	messageLog.setStringProperty("pernrlist valid from EC: ", pernrList.toString());
	messageLog.setStringProperty("Userlist : ", userIdList.toString());
	
	List abhList = pmap.get("ABH_LIST");
	
	String eeList= "";
	
	for(item in abhList){
	        eeList += item.personIdExternal+"\n";
	        JobData eeJob = jobMap.get(item.personIdExternal);
	        userList += eeJob.toString()+"\n"
	 }
	 
	 String eeList1 = pmap.get("PERSON_ID_EXTERNAL_PARAMETER");	
	 String eeListUser = pmap.get("USER_TEST_EE");	
	 String[] pernrs = null;
	 if(eeListUser.length()> 0){ 
	     pernrs = eeListUser.split(",");
	 }else{
	     pernrs = eeList1.split(",");
	 }
	 
	List<String> inputPernrList = new ArrayList<>(Arrays.asList(pernrs));
	List<String> pernrNotInECList = new ArrayList<>(Arrays.asList(pernrs));

	Collections.sort(inputPernrList);
	messageLog.setStringProperty("pernr from SAP: ", inputPernrList.toString());
	inputPernrList.retainAll(pernrList);
	pernrNotInECList.removeAll(pernrList);
	messageLog.setStringProperty("pernr not in EC: ", pernrNotInECList.toString());
	
	messageLog.setStringProperty("sequence map: ", sequenceMap.toString());
	
	HashMap<String, UpsertXML> XMLMap = new HashMap<String, UpsertXML>();
	List<NotifyItem> notifyList = new ArrayList<NotifyItem>();
	
	int eeCount = 0;
	
		for( item in abhList){
			if(inputPernrList.contains(item.personIdExternal)){
				eeCount++;
				UpsertXML uxml;
				if(XMLMap.containsKey(item.personIdExternal)){
				    uxml = XMLMap.get(item.personIdExternal);
				    
				}else{
				    uxml = new UpsertXML(); 
				}

 
				 // job upsert XML
				 JobData eeJob = jobMap.get(item.personIdExternal);
				
				 String jobKey = item.personIdExternal+"J"+item.startDate;
				 String seqNumber ="";
				 seqNumber = (sequenceMap.get(jobKey)==null?"1":sequenceMap.get(jobKey)+"");

				 payload=""
			     payload += "<EmpJob>";  
			     payload += "<startDate>"+item.startDate+"T00:00:00.000Z"+"</startDate>";
			     payload += "<eventReason>"+item.eventReason+"</eventReason>";
			     payload += "<payScaleGroup>"+item.payScaleGroup+"</payScaleGroup>";
			     payload += "<payScaleArea>"+item.payScaleArea+"</payScaleArea>";
			     payload += "<userId>"+eeJob.user_id+"</userId>";
			     payload += "<payScaleType>"+item.payScaleType+"</payScaleType>";
			     payload += "<payScaleLevel>"+item.payScaleLevel+"</payScaleLevel>";
			     payload += "<seqNumber>"+seqNumber+"</seqNumber>";
				 payload += "<customString9>"+eeJob.custom_string9+"</customString9>";   
				 payload += "<customString22>"+eeJob.custom_string22+"</customString22>";
				 payload += "<endDate>"+item.endDate+"</endDate>";
			     payload += "</EmpJob>";    
			     uxml.jobXML = uxml.jobXML + payload;
			     
			     
			     //paycomp xml
			     //userId,startDate,eventReason,endDate
			     payload="";
			     payload += "<EmpCompensation>";  
			     payload += "<userId>"+eeJob.user_id+"</userId>";
			     payload += "<startDate>"+item.startDate+"T00:00:00.000Z"+"</startDate>";
			     payload += "<eventReason>"+item.eventReason+"</eventReason>";
			     payload += "<endDate>"+item.endDate+"</endDate>";
			     payload += "</EmpCompensation>";  
			     uxml.compXML += payload;
			     //pay recurring basic pay xml
			     //userId,startDate,seqNumber,paycompvalue,payComponent,endDate
			     payload="";
			    
			     String payKey = item.personIdExternal+"P"+"1005"+item.startDate;
			      seqNumber ="";
			     if(sequenceMap.get(payKey)==null){
			         seqNumber = "1";
			     }else{
			         seqNumber = Integer.parseInt(sequenceMap.get(payKey))+1+"";
			         
			     }


				// seqNumber = (sequenceMap.get(payKey)==null?"1":sequenceMap.get(payKey));
				 
			     
			     payload += "<EmpPayCompRecurring>";  
			     payload += "<userId>"+eeJob.user_id+"</userId>";
			     payload += "<startDate>"+item.startDate+"T00:00:00.000Z"+"</startDate>";
			     payload += "<seqNumber>" + seqNumber + "</seqNumber>";
			     payload += "<paycompvalue>"+ item.hourlyRate +"</paycompvalue>"
			     payload += "<payComponent>1005</payComponent>";
			    // payload += "<endDate>"+item.endDateMS+"</endDate>";
			     payload += "</EmpPayCompRecurring>";  
			     uxml.payRecurBasicXML += payload;
			     //messageLog.addAttachmentAsString("xml", uxml.payRecurBasicXML, "text/xml");
			     //pay recurring premium pay xml
			     //userId,startDate,seqNumber,paycompvalue,payComponent,endDate
			     if( Double.valueOf(item.premiumRate)>0) {
			          String preKey = item.personIdExternal+"P"+"1010"+item.startDate;
			     seqNumber ="";
			     if(sequenceMap.get(payKey)==null){
			         seqNumber = "1";
			     }else{
			         seqNumber = Integer.parseInt(sequenceMap.get(payKey))+1+"";
			         
			     }
			         payload="";
				     payload += "<EmpPayCompRecurring>";  
				     payload += "<userId>"+eeJob.user_id+"</userId>";
				     payload += "<startDate>"+item.startDate+"T00:00:00.000Z"+"</startDate>";
				     payload += "<seqNumber>" + seqNumber + "</seqNumber>";
				     payload += "<paycompvalue>"+ item.premiumRate +"</paycompvalue>"
				     payload += "<payComponent>1010</payComponent>";
				     //payload += "<endDate>"+item.endDateMS+"</endDate>";
				     payload += "</EmpPayCompRecurring>"; 
				    
				     uxml.payRecurPremXML += payload;
				     }

			    
			     XMLMap.put(item.personIdExternal,uxml);
			     
			     //generate Notify list
			     NotifyItem notify = new NotifyItem();
			     notify.personnelNumber= item.personIdExternal;
			     notify.userId = eeJob.user_id;
			     notify.endDate = item.endDate+"T23:59:59Z";
			     notify.beginDate = item.startDate+"T00:00:00Z";
			     notify.changedOnDate = item.changedOnDate;
			     notify.status = "1";
			     notifyList.add(notify);
			     
			} 
	 }
	
	message.setProperty("XML_LIST", XMLMap);
	
	message.setProperty("JOB_MAP", jobMap);
	
	message.setProperty("NOTIFY_LIST",notifyList);
	
	message.setProperty("EE_COUNT", eeCount);
	
	message.setBody("<EmpJob>" + "dummy" + "</EmpJob>");
	
	return message;
}

