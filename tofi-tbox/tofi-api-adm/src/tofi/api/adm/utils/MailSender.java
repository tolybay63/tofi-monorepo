package tofi.api.adm.utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jandcode.commons.error.XError;
import jandcode.core.dbm.mdb.Mdb;
import jandcode.core.std.CfgService;

import java.util.Properties;

public class MailSender {
    private final String username = "dtj.service360@gmail.com";
    private String mailPass="";
    Mdb mdb;


    private final Properties props;

    public MailSender(Mdb mdb) {
        this.mdb = mdb;
        props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        CfgService cfgSvc = mdb.getApp().bean(CfgService.class);
        mailPass = cfgSvc.getConf().getString("mail/gmail/password", "");
        if (mailPass.isEmpty()) {
            throw new XError("Ошибка настройки SMTP");
        }
    }

    public String send(String subject, String text, String toEmail){
        String result_sending = "OK";
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, mailPass);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            //от кого
            message.setFrom(new InternetAddress(username));
            //кому
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            //Заголовок письма
            message.setSubject(subject);
            //Содержимое
            message.setText(text, "utf-8", "html");

            //Отправляем сообщение
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return result_sending;
    }
}