package gr.cti.gaia.dataset.exporter.service;

import gr.cti.gaia.dataset.exporter.model.Metrics;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class MetricsService {
    
    @PostConstruct
    public void init() {
    }
    
    public boolean checkValue(ResourceDTO resourceName, Double value, Metrics currentMetrics, StringBuilder line) {
        if ("noise".equals(resourceName)) {
            line.append(",").append(value> 85 ? "0" : "1");
            if (value> 85) {
                currentMetrics.incAbnormalHigh();
            }
        } else if ("luminosity".equals(resourceName)) {
            line.append(",").append(value < 150 ? "0" : "1");
            if (value< 150) {
                currentMetrics.incAbnormalLow();
            }
        } else if ("humidity".equals(resourceName)) {
            if (value == 0) {
                return true;
            }
            line.append(",").append(value < 40 || value > 60 ? "0" : "1");
            if (value < 40) {
                currentMetrics.incAbnormalLow();
            }
            if (value > 60) {
                currentMetrics.incAbnormalHigh();
            }
        } else if ("temperature".equals(resourceName)) {
            if (value == 0) {
                return true;
            }
            line.append(",").append(value < 19 || value > 28 ? "0" : "1");
            if (value < 19) {
                currentMetrics.incAbnormalLow();
            }
            if (value > 28) {
                currentMetrics.incAbnormalHigh();
            }
        }
        return false;
    }
}
