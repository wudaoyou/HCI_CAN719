import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import java.lang.Exception;




def Message processData(Message message) {

	def pmap = message.getProperties();
	String delaySeconds = pmap.get("DELAY_SECONDS");
	long milliseconds = 0;
	if(delaySeconds != null ){
		long milliseconds = Integer.parseInt(seconds)*1000;
	}
	
	sleep(milliseconds);
	   
	return message;
}

