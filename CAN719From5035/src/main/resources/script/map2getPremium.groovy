import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
import groovy.xml.*;
import groovy.util.slurpersupport.*;
import java.text.SimpleDateFormat;

class Param {
	String Pernr;
	String Bukrs;
	String Country;
	String ZzWorkcenter;
	String Stell;
	String ZzLocType;
	String locSubtype;
	String Zpersarea;
	String Btrtl;
	String Trfar;
	String Trfgb;
	String Trfgr;
	String Trfst;
	String Persg;
	String Persk;
	String Begda;
	String Endda;
	String Lgart;
	String Betrg;
	String pay_seqno;
	String pay_startdt;
}

def Message processData(Message message) {

	//Body
	String inXML = message.getBody(java.lang.String) as String;
	def body = new XmlSlurper().parseText(inXML);
	
	//header
	def pmap = message.getProperties();
		
	// Logs
	def messageLog = messageLogFactory.getMessageLog(message);
	
	
	String enableLogging = pmap.get("ENABLE_LOGGING");	
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	

	
		// beging mapping
	def compoundEEs = body.CompoundEmployee;
	messageLog.setStringProperty("compoundEEs - class ", compoundEEs.getClass().toString());
	
	List eeList = pmap.get("PERSON_ID_LIST");
	List abhList = pmap.get("ABH_LIST");
	List<Param> paramList = new ArrayList<Param>();
	
	String payload = "";
	HashMap<String, NodeChild> eeMap = new HashMap<String, NodeChild>();

	compoundEEs.each{
	    messageLog.setStringProperty("compoundEE child - class ", it.getClass().toString());
	    String pernr = it.person.person_id_external;
	    eeMap.put(pernr, it);
	}
	
	for(abh in abhList){
	     def compEE = eeMap.get(abh.personIdExternal); 
	     Date startDateABH = formatter.parse(abh.startDate);
	     Date endDateABH = formatter.parse(abh.endDate);
	    
	     Param p = null;      
	     for (def emp in compEE.person.employment_information) {    
	      	for (def job in emp.job_information) {
	      	 	   Date startDateXML = formatter.parse(job.start_date.toString());
	      	 	    Date endDateXML = formatter.parse(job.end_date.toString());
	      	 	   if (startDateXML.compareTo(startDateABH)<=0 && 
	      	 	   	   endDateXML.compareTo(endDateABH)>=0 && 
	      	 	   	   job.emplStatus == 'A'){
	      	 	   	   p = new Param();
	      	 	       p.Pernr = abh.personIdExternal;
	      	 	       p.Bukrs = job.company;
	      	 	       p.Country = job.company_territory_code;
	      	 	       p.Stell = job.job_code;
					   p.ZzWorkcenter = job.location;
					   p.ZzLocType = job.custom_string12;
					   p.locSubtype = job.custom_string23;
					   p.Zpersarea = job.custom_string15;
					   p.Btrtl = job.custom_string16;
					   p.Trfar = abh.payScaleType;
					   p.Trfgb = abh.payScaleArea;
					   p.Trfgr = abh.payScaleGroup;
					   p.Trfst = abh.payScaleLevel;   
					   p.Persg = job.employee_class;
					   p.Persk = job.employment_type; 
					   p.Begda = abh.startDate;
					   p.Endda = abh.endDate;	 
					   p.Lgart = "1010"; 
					   p.Betrg = 0;
					   p.pay_startdt = abh.startDate;
					   p.pay_seqno = "1";                                                                                         
	      	 	                                                                                                       
	      	 	 }                   
	      	}

	     } 
	     if (p != null && p.Lgart == "1010") {
					// add pernr
					payload = payload + "<EmployeeNumber>" + p.Pernr + "</EmployeeNumber>";
					// add bukrs
					payload = payload + "<CompanyCode>" + p.Bukrs + "</CompanyCode>";
					// add country
					payload = payload + "<CountryCode>" + p.Country + "</CountryCode>";
					// add work center
					payload = payload + "<LocationCode>" + p.ZzWorkcenter + "</LocationCode>";
					// add stell
					payload = payload + "<JobCode>" + p.Stell + "</JobCode>";
					// add location type
					payload = payload + "<LocationType>" + p.ZzLocType + "</LocationType>";
					// add location sub type
					payload = payload + "<LocationSubType>" + p.locSubtype + "</LocationSubType>";
					// add persa
					payload = payload + "<PersonnelArea>" + p.Zpersarea + "</PersonnelArea>";
					// add Btrtl
					payload = payload + "<PersonnelSubArea>" + p.Btrtl + "</PersonnelSubArea>";
					// add pay scale parameters
					def v = p.Trfst.split("/");
					p.Trfst = v[v.length-1];
					v = p.Trfgr.split("/");
					p.Trfgr = v[v.length - 1];
					v = p.Trfar.split("/");
					p.Trfar = v[v.length-1];
					v= p.Trfgb.split("/");
					p.Trfgb = v[v.length-1];
					
					payload = payload + "<PayScaleType>" + p.Trfar + "</PayScaleType>";
					payload = payload + "<PayScaleArea>" + p.Trfgb + "</PayScaleArea>";
					payload = payload + "<PayScaleGroup>" + p.Trfgr + "</PayScaleGroup>";
					payload = payload + "<PayScaleLevel>" + p.Trfst + "</PayScaleLevel>";
					// add employee group/subgroup
					payload = payload + "<EmployeeGroup>" + p.Persg + "</EmployeeGroup>";
					payload = payload + "<EmployeeSubGroup>" + p.Persk + "</EmployeeSubGroup>";
					// add beg/end date
					payload = payload + "<BeginDate>" + p.Begda + "</BeginDate>";
					payload = payload + "<EndDate>" + p.Endda + "</EndDate>";
					// add wage info
					payload = payload + "<WageType>" + p.Lgart + "</WageType>";
					payload = payload + "<WageTypeAmount>" + p.Betrg + "</WageTypeAmount>";
					payload = payload + "<PaymentStartDate>" + p.pay_startdt + "</PaymentStartDate>";
					payload = payload + "<PaymentSequenceNumber>" + p.pay_seqno + "</PaymentSequenceNumber>";
				} // end of if lgart == 1010
	 }

	message.setBody("<Request>" + payload + "</Request>");
	
	
	return message;
}