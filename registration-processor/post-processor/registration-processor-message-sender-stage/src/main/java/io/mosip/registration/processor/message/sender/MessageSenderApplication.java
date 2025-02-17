package io.mosip.registration.processor.message.sender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.mosip.registration.processor.message.sender.stage.MessageSenderApi;
import io.mosip.registration.processor.message.sender.stage.MessageSenderStage;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The Class MessageSenderApplication.
 *
 * @author Alok Ranjan
 * @since 1.0.0
 *
 */
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
@ComponentScan(basePackages = { "${app.componentscan.basepackages}" })
public class MessageSenderApplication {
	/**
	 * Main method to instantiate the spring boot application.
	 *
	 * @param args
	 *            the command line arguments
	 */
	@SuppressWarnings("resource")
	public static void main(String[] args) {


		SpringApplication.run(MessageSenderApplication.class, args);

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan("io.mosip.registration.processor.core.config",
				"io.mosip.registration.processor.stages.uingenerator.config",
				"io.mosip.registration.processor.status.config", "io.mosip.registration.processor.rest.client.config",
				"io.mosip.registration.processor.packet.storage.config",
				"io.mosip.registration.processor.stages.config",
				"io.mosip.registration.processor.message.sender.config",
				"io.mosip.registration.processor.packet.manager.config",
				"io.mosip.registration.processor.core.kernel.beans",
				"io.mosip.kernel.packetmanager.config");

		ctx.refresh();

		MessageSenderStage messageSenderStage = ctx.getBean(MessageSenderStage.class);
		messageSenderStage.deployVerticle();

		MessageSenderApi messageSenderApi = ctx.getBean(MessageSenderApi.class);
		messageSenderApi.deployVerticle();


	}

}