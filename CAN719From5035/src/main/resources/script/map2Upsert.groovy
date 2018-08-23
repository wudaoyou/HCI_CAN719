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
import java.text.SimpleDateFormat;

class UpsertItem implements Comparable<UpsertItem>{
    String personIdExternal;
	String userId;
	String startDate;
	String startDateMS
	String endDate;
	String endDateMS;
	String eventReason;
	String payScaleGroup;
	String payScaleArea;
	String payScaleType;
	String payScaleLevel;
	String customString9 ;
	String seqNumber;
	String hourlyRate;
	String annualRate;
	String premiumRate;
	String changedOnDate;
	
	public String getSeqNumber() {
		return seqNumber;
	}
	
	public int compareTo(UpsertItem compareItem) {
		
			String compareSeqNumber = ((UpsertItem) compareItem).getSeqNumber();
			
			//ascending order
			return this.seqNumber.compareTo(compareSeqNumber);
			
			//descending order
			//return compareQuantity - this.quantity;
			
		}
}

def Map<String,String> getReasonFromNumber(){
	Map<String,String> reasonCodeMap = new HashMap<>();
	reasonCodeMap.put("01","PCAUTOB");
	reasonCodeMap.put("03","PCANNINC");
	reasonCodeMap.put("04","PCADJUST");
	reasonCodeMap.put("05","PCMERIT");
	reasonCodeMap.put("06","PCRATCHG");
	reasonCodeMap.put("07","PCSCLCHG");
	reasonCodeMap.put("08","PCHRSRES");
	reasonCodeMap.put("09","PCHRSRET");
	reasonCodeMap.put("99","OTHER");
	return reasonCodeMap;
 }

def Message processData(Message message) {

	List<UpsertItem> upsertItems = new ArrayList<UpsertItem>();
	Map<String,String> reasonMap = getReasonFromNumber();
	
	def pmap = message.getProperties();
	String enableLogging = pmap.get("ENABLE_LOGGING");
	
	def messageLog = messageLogFactory.getMessageLog(message);
		
	String inXML = message.getBody(java.lang.String) as String;
	def body = new XmlSlurper().parseText(inXML).declareNamespace(env:'http://www.w3.org/2003/05/soap-envelope');
	body.declareNamespace(n0:"http://costco.com/svc/businessAdministrationAndFinancials/HumanResources/AutoPayIncrease/v1/");

	
	String payload = "";
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	    
	
	def results = body.'env:Body'.'n0:loadResponse'.Response.LoadEmployee;
	
	results.each {  
		UpsertItem item = new UpsertItem();
		//set user id
		item.personIdExternal = it.PersonnelNumber;
		//set startDate
		//2017-11-04T19:41:17Z
		String dateStr = it.BeginDate;
		item.startDate =  dateStr.substring(0,10);
		Date date = sdf.parse(item.startDate);
		long millis = date.getTime();
		item.startDateMS =  "Date(" + millis + ")";
		dateStr="";
		dateStr = it.EndDate;
		item.endDate =  dateStr.substring(0,10);
		date = sdf.parse(item.endDate);
		millis = date.getTime();
		item.endDateMS =  "Date(" + millis + ")";
		
		dateStr = it.ChangedOnDate;
		item.changedOnDate = dateStr;
		def(reason, seqnr) = it.Reason.toString().tokenize('-');
		//String[] reasonSeqnr = it.Reason.toString().split('-');
		
		
		//set payscale
		//example: CAN/15/Z5/20160314/01
		//	String payScale = "CAN/"+it.PayScaleArea+"/"+it.PayScaleType+"/"+it.PayScaleGroup+"/"+it.PayScaleLevel;
		item.payScaleArea = "CAN/"+it.PayScaleArea;
		item.payScaleType = "CAN/"+it.PayScaleType;
		item.payScaleGroup = item.payScaleArea+ "/"+it.PayScaleType+"/"+it.PayScaleGroup;
		item.payScaleLevel = item.payScaleGroup+"/"+it.PayScaleLevel;
		item.hourlyRate = it.HourlyRate;
		item.annualRate = it.AnnualRate;
		item.premiumRate = it.PremiumRate;
		item.seqNumber = seqnr;
		item.eventReason = reasonMap.get(reason);
		//item.eventReason = reason.equals("01") ? "PCAUTOB" : "OTHER";
		
		
		upsertItems.add(item);
	}
	//Collections.sort(upsertItems);
	message.setProperty("ABH_LIST", upsertItems);
	

	return message;
}

