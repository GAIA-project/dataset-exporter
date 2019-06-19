package gr.cti.gaia.dataset.exporter.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataPoint {
    private long timestamp;
    private Double value;
}

