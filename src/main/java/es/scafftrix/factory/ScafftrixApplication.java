package es.scafftrix.factory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ScafftrixApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScafftrixApplication.class, args);
    }
}