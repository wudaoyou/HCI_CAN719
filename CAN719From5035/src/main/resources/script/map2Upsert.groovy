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

class UpsertItem {
    String personIdExternal;
	String userId;
	String startDate;
	String endDate;
	String eventReason = "PCAUTOB";
	String payScaleGroup;
	String payScaleArea;
	String payScaleType;
	String payScaleLevel;
	String customString9 = "00000518" ;
	String seqNumber= "1";
	String hourlyRate;
	String annualRate;
	String premiumRate;
	String changedOnDate;
}

def Message processData(Message message) {

	List<UpsertItem> upsertItems = new ArrayList<UpsertItem>();
	
	def pmap = message.getProperties();
	String enableLogging = pmap.get("ENABLE_LOGGING");
	
	def messageLog = messageLogFactory.getMessageLog(message);
		
	String inXML = message.getBody(java.lang.String) as String;
	def body = new XmlSlurper().parseText(inXML).declareNamespace(env:'http://www.w3.org/2003/05/soap-envelope');
	body.declareNamespace(n0:"http://costco.com/svc/businessAdministrationAndFinancials/HumanResources/AutoPayIncrease/v1/");

	
	String payload = "";
	
	def results = body.'env:Body'.'n0:loadResponse'.Response.LoadEmployee;
	
	results.each {  
		UpsertItem item = new UpsertItem();
		//set user id
		item.personIdExternal = "0"+it.PersonnelNumber;
		//set startDate
		//2017-11-04T19:41:17Z
		String dateStr = it.BeginDate;
		item.startDate =  dateStr.substring(0,10);
		dateStr="";
		dateStr = it.EndDate;
		item.endDate =  dateStr.substring(0,10);
		dateStr = it.ChangedOnDate;
		item.changedOnDate = dateStr;
		
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
		if(it.PersonnelNumber.equals("5028420")){
		    messageLog.setStringProperty("PersonnelNumber: ", it.PersonnelNumber.toString());
		    messageLog.setStringProperty("HourlyRate: ", it.HourlyRate.toString());
		    messageLog.setStringProperty("AnnualRate: ", it.AnnualRate.toString());
		    messageLog.setStringProperty("premiumRate: ", it.PremiumRate.toString());
		}

		
		upsertItems.add(item);
	}
	message.setProperty("ABH_LIST", upsertItems);
	

	return message;
}

