package gr.cti.gaia.dataset.exporter;

import net.sparkworks.cargo.client.config.CargoClientConfig;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

@SpringBootApplication(scanBasePackages = {"gr.cti.gaia.dataset.exporter", CargoClientConfig.CARGO_CLIENT_BASE_PACKAGE_NAME})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Bean(name = "buildingStudents")
    public static PropertiesFactoryBean buildingStudents() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("students.properties"));
        return bean;
    }
    
    @Bean(name = "roomFacing")
    public static PropertiesFactoryBean roomFacing() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("facing.properties"));
        return bean;
    }
    
    @Bean(name = "floorLevels")
    public static PropertiesFactoryBean floorLevels() {
        PropertiesFactoryBean bean = new PropertiesFactoryBean();
        bean.setLocation(new ClassPathResource("level.properties"));
        return bean;
    }
}
