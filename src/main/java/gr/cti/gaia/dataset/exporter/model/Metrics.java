package gr.cti.gaia.dataset.exporter.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Metrics {
    private Double total = 0.0;
    private Double abnormalLow = 0.0;
    private Double abnormalHigh = 0.0;
    
    public void incTotal() {
        total++;
    }
    
    public void incAbnormalLow() {
        abnormalLow++;
    }
    
    public void incAbnormalHigh() {
        abnormalHigh++;
    }
    
}
