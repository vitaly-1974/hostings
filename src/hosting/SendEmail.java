package hosting;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.*;

public class SendEmail {

final String senderEmailID = "vitaly.missing@oracle.com";
final String senderPassword = "Vm----1974";
final String emailSMTPserver = "stbeehive.oracle.com";
final String emailServerPort = "465";
String receiverEmailID = null;
static String emailSubject = "Test Mail";
static String emailBody = ":)";

public SendEmail(String receiverEmailID, String emailSubject, String emailBody){
this.receiverEmailID=receiverEmailID;
this.emailSubject=emailSubject;
this.emailBody=emailBody;
Properties props = new Properties();
props.put("mail.smtp.user",senderEmailID);
props.put("mail.smtp.host", emailSMTPserver);
props.put("mail.smtp.port", emailServerPort);
props.put("mail.smtp.starttls.enable", "true");
props.put("mail.smtp.auth", "true");
props.put("mail.smtp.socketFactory.port", emailServerPort);
props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
props.put("mail.smtp.socketFactory.fallback", "false");
SecurityManager security = System.getSecurityManager();
try {
Authenticator auth = new SMTPAuthenticator();
Session session = Session.getInstance(props, auth);
MimeMessage msg = new MimeMessage(session);
msg.setText(emailBody);
msg.setSubject(emailSubject);
msg.setFrom(new InternetAddress(senderEmailID));
msg.addRecipient(Message.RecipientType.TO,
new InternetAddress(receiverEmailID));
Transport.send(msg);
System.out.println("Message send Successfully:)");
} 
catch (Exception mex)
{mex.printStackTrace();}
}

public class SMTPAuthenticator extends javax.mail.Authenticator{
 public PasswordAuthentication getPasswordAuthentication(){
  return new PasswordAuthentication(senderEmailID, senderPassword);
 }
}
    public static void main(String[] args) {
       SendEmail mailSender;
       mailSender = new SendEmail("vitaly.missing@oracle.com","Suspisious hosts","Testing Code Body yess");
    }

}