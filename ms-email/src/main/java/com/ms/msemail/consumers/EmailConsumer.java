package com.ms.msemail.consumers;

import com.ms.msemail.dtos.EmailRecordDto;
import com.ms.msemail.models.Email;
import com.ms.msemail.services.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    final EmailService emailService;

    public EmailConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${broker.queue.email.name}")
    public void listenEmailQueue(@Payload EmailRecordDto emailRecordDto){
        var emailModel = new Email();
        BeanUtils.copyProperties(emailRecordDto,emailModel);
        emailService.sendEmail(emailModel);
    }
}
