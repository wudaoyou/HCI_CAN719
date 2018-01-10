import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.*;
def void updateNotifyList(Message message){
def pmap = message.getProperties();
		List notifyList =pmap.get("NOTIFY_LIST");
		
		for(item in notifyList){
		    if(!(item.status.equals("4"))){
    			item.status = "0";
    			}    
		}
		message.setProperty("NOTIFY_LIST",notifyList);
 }

def Message processData(Message message) {
  updateNotifyList(message);
  
	return message;
}