package pl.edu.agh.sr.rabbitmq.communicationplatform.employees;

import com.rabbitmq.client.*;
import pl.edu.agh.sr.rabbitmq.communicationplatform.Department;
import pl.edu.agh.sr.rabbitmq.communicationplatform.ui.frame.DoctorFrame;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class DoctorThread extends EmployeeThread {
    private String callbackQueueName;

    public DoctorThread(Department department, String name) {
        super(department, name);
        specializations.addAll(Arrays.asList(Department.specializations));
    }

    @Override
    public void run() {
        ui = new DoctorFrame(this);
        init();
        try {
            callbackQueueName = getName() + "-callback-queue";

            channel.queueDeclare(callbackQueueName, true, false, false, null);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body)
                        throws IOException {
                    if (properties != null && properties.getCorrelationId() != null
                            && properties.getCorrelationId().startsWith(getName())) {
                        String message = new String(body, "UTF-8");
                        ui.printMessage(message);
                    }
                }
            };
            channel.basicConsume(callbackQueueName, true, consumer);
        } catch (IOException e) {
            department.log(
                    "<< " + this.getName() + " | error while setting up (Exception caught: " + e + "). >>"
            );
            e.printStackTrace();
        }
    }

    public void orderTest(String type, String patient) {
        try {
            String corrID = getName() + UUID.randomUUID().toString();

            AMQP.BasicProperties props = new AMQP
                    .BasicProperties.Builder()
                    .correlationId(corrID)
                    .replyTo(callbackQueueName)
                    .build();

            String message = getName() + "&" + patient;
            channel.basicPublish("", type, props, message.getBytes());
            log("Log << [" + getName() + "] " + message + " >>");
            ui.printMessage("> Ordered test, type: " + type + ", patient: " + patient + ", test ID: " + corrID);
        } catch (IOException e) {
            department.log(
                    "<< " + this.getName() + " | error while ordering a test (Exception caught: " + e + "). >>"
            );
        }
    }
}
