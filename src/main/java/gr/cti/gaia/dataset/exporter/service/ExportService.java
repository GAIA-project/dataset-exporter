package gr.cti.gaia.dataset.exporter.service;

import gr.cti.gaia.dataset.exporter.dto.DataPoint;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.common.dto.GroupDTO;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import net.sparkworks.cargo.common.dto.data.QueryTimeRangeResourceDataResultDTO;
import net.sparkworks.cargo.common.dto.data.ResourceDataDTO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Slf4j
@Component
@ConfigurationProperties("export")
public class ExportService {
    
    private final GroupService groupService;
    private final DataService dataService;
    
    private List<String> uuids;
    private Long from;
    private Long to;
    
    @Autowired
    public ExportService(final GroupService groupService, final DataService dataService) {
        this.groupService = groupService;
        this.dataService = dataService;
    }
    
    public void setUuids(final List<String> uuids) {
        this.uuids = uuids;
    }
    
    public void setFrom(final Long from) {
        this.from = from;
    }
    
    public void setTo(final Long to) {
        this.to = to;
    }
    
    @PostConstruct
    public void init() {
        log.info("uuids: {}", uuids);
        log.info("from: {} to: {}", from, to);
        
        for (final String uuid : uuids) {
            final GroupDTO groupDTO = groupService.findByUUID(uuid);
            exportDatasetForGroup(groupDTO, from, to);
        }
    }
    
    private void exportDatasetForGroup(final GroupDTO groupDTO, final Long from, final Long to) {
        log.info("===================================================================");
        log.info("groupDTO: {} from: {} to: {}", groupDTO, from, to);
        
        final Map<String, GroupDTO> areas = buildFileStructure(groupDTO);
        for (final String area : areas.keySet()) {
            final GroupDTO areaDTO = areas.get(area);
            final Map<String, ResourceDTO> resources = new HashMap<>();
            resources.put("temperature", groupService.getTemperatureResource(areaDTO));
            resources.put("humidity", groupService.getRelativeHumidityResource(areaDTO));
            resources.put("luminosity", groupService.getLuminosityResource(areaDTO));
            resources.put("current", groupService.getElectricalCurrentResource(areaDTO));
            resources.put("power", groupService.getPowerConsumptionResource(areaDTO));
            resources.put("occupancy", groupService.getOccupancyResource(areaDTO));
            for (final String resourceName : resources.keySet()) {
                final ResourceDTO resource = resources.get(resourceName);
                if (resource == null) {
                    continue;
                }
                log.info("{}: {}", resourceName, resource);
                File f = new File(area + "/" + resourceName + ".csv");
                if (!f.exists()) {
                    try {
                        f.createNewFile();
                    } catch (IOException e) {
                        log.error(e.getLocalizedMessage(), e);
                    }
                }
                getData(resourceName, resource, f, from, to);
            }
            
        }
        
        
    }
    
    private void getData(final String resourceName, final ResourceDTO resource, final File f, final Long fromTotal, final Long toTotal) {
        double diff = toTotal - fromTotal;
        final Calendar calendarFrom = Calendar.getInstance();
        calendarFrom.setTimeInMillis(fromTotal);
        final SortedMap<Long, DataPoint> dataPoints = new TreeMap<>();
        do {
            final long from = calendarFrom.getTimeInMillis();
            log.info("{}\t{}\t{}", resourceName, String.format("%.0f%%", ((from - fromTotal) / diff) * 100), calendarFrom.getTime());
            calendarFrom.add(Calendar.DAY_OF_YEAR, 1);
            final long to = calendarFrom.getTimeInMillis();
            {
                final QueryTimeRangeResourceDataResultDTO data = dataService.getData(resource, from, to);
                for (final ResourceDataDTO datum : data.getResults().values().iterator().next().getData()) {
                    if (datum.getTimestamp() > toTotal) {
                        continue;
                    }
                    if (!dataPoints.containsKey(datum.getTimestamp())) {
                        dataPoints.put(datum.getTimestamp(), DataPoint.builder().timestamp(datum.getTimestamp()).build());
                    }
                    dataPoints.get(datum.getTimestamp()).setValue(datum.getReading());
                }
            }
        } while (calendarFrom.getTimeInMillis() < toTotal);
        log.info("-------------------------------------------------------------------");
        
        try (final PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
            pw.append("Epoch,Year,Month,Day,Hour,Minute,").append(StringUtils.capitalize(resourceName)).append("\n");
            for (final DataPoint value : dataPoints.values()) {
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(value.getTimestamp());
                pw.append(String.valueOf(cal.getTimeInMillis())).append(",").append(String.valueOf(cal.get(Calendar.YEAR))).append(",").append(String.valueOf(cal.get(Calendar.MONTH) + 1)).append(",").append(String.valueOf(cal.get(Calendar.DAY_OF_MONTH))).append(",").append(String.valueOf(cal.get(Calendar.HOUR_OF_DAY))).append(",").append(String.valueOf(cal.get(Calendar.MINUTE))).append(",").append(String.format("%.2f", value.getValue())).append("\n");
            }
            pw.flush();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
    
    private Map<String, GroupDTO> buildFileStructure(final GroupDTO groupDTO) {
        final Map<String, GroupDTO> areas = new HashMap<>();
        try {
            final String buildingId = exportLastPathParam(groupDTO);
            final String buildingPath = "building" + buildingId;
            areas.put(buildingPath, groupDTO);
            FileUtils.forceMkdir(new File(buildingPath));
            final FileWriter fw = new FileWriter(new File(buildingPath + "/description.txt"));
            fw.append("Building: ").append(buildingId).append("\n");
            fw.append("current: mA\n");
            fw.append("power: mWh\n");
            fw.append("temperature: C\n");
            fw.append("humidity: %RH\n");
            fw.append("luminosity: lux\n");
            fw.append("occupancy: 0 empty, 1 full, 0.xx intermediate\n");
            Collection<GroupDTO> subGroups = groupService.getSubGroups(groupDTO);
            for (GroupDTO subGroup : subGroups) {
                log.info("subGroup: {}", subGroup);
                final String subAreaId = exportLastPathParam(subGroup);
                fw.append("Floor: ").append(subAreaId).append("\n");
                fw.append("\t level: \n");
                final String subGroupPath = "building" + buildingId + "/floor" + subAreaId;
                areas.put(subGroupPath, subGroup);
                FileUtils.forceMkdir(new File(subGroupPath));
                Collection<GroupDTO> floorSubGroups = groupService.getSubGroups(subGroup);
                for (GroupDTO floorSubGroup : floorSubGroups) {
                    log.info("floorSubGroup: {}", floorSubGroup);
                    final String floorSubAreaId = exportLastPathParam(floorSubGroup);
                    fw.append("Room: ").append(floorSubAreaId).append("\n");
                    fw.append("\t facing: \n");
                    fw.append("\t usage: \n");
                    final String floorSubGroupPath = "building" + buildingId + "/floor" + subAreaId + "/room" + floorSubAreaId;
                    areas.put(floorSubGroupPath, floorSubGroup);
                    FileUtils.forceMkdir(new File(floorSubGroupPath));
                }
            }
            fw.close();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return areas;
    }
    
    private String exportLastPathParam(GroupDTO groupDTO) {
        return groupDTO.getPath().substring(groupDTO.getPath().lastIndexOf(".") + 1);
    }
    
    
}
