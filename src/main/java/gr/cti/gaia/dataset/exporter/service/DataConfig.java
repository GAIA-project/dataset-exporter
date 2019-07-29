package gr.cti.gaia.dataset.exporter.service;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("export.data")
@ToString
public class DataConfig {
    private Boolean temperature;
    private Boolean humidity;
    private Boolean luminosity;
    private Boolean noise;
    private Boolean power;
    private Boolean current;
    private Boolean occupancy;
}
