package gr.cti.gaia.dataset.exporter;

import net.sparkworks.cargo.client.config.CargoClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"gr.cti.gaia.dataset.exporter", CargoClientConfig.CARGO_CLIENT_BASE_PACKAGE_NAME})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
