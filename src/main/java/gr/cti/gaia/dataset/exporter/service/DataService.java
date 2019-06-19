package gr.cti.gaia.dataset.exporter.service;

import net.sparkworks.cargo.client.DataClient;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.cargo.common.dto.data.Granularity;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataCriteriaDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class DataService {
    
    private final DataClient dataClient;
    
    @Autowired
    public DataService(final DataClient dataClient) {
        this.dataClient = dataClient;
    }
    
    public QueryTimeRangeResourceDataResultDTO getData(ResourceDTO temperature, long from, long to) {
        QueryTimeRangeResourceDataDTO dto = new QueryTimeRangeResourceDataDTO();
        dto.setQueries(new ArrayList<>());
        QueryTimeRangeResourceDataCriteriaDTO criteria = new QueryTimeRangeResourceDataCriteriaDTO();
        criteria.setFrom(from);
        criteria.setTo(to);
        criteria.setGranularity(Granularity.MIN_5);
        criteria.setResourceUuid(temperature.getUuid());
        criteria.setResourceURI(temperature.getSystemName());
        dto.getQueries().add(criteria);
        return dataClient.queryTimeRangeResourcesData(dto);
    }
}
