package gr.cti.gaia.dataset.exporter.service;

import gr.cti.gaia.dataset.exporter.dto.DataPoint;
import gr.cti.gaia.dataset.exporter.model.Metrics;
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
import javax.annotation.Resource;
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

import static java.util.Calendar.SUNDAY;

@Slf4j
@Component
@ConfigurationProperties("export")
public class ExportService {
    
    private final GroupService groupService;
    private final DataService dataService;
    private final MetricsService metricsService;
    
    private List<String> uuids;
    private Long from;
    private Long to;
    private Boolean drillDown;
    private Boolean metrics;
    
    @Resource(name = "buildingStudents")
    private Map<String, String> buildingStudents;
    @Resource(name = "roomFacing")
    private Map<String, String> roomFacing;
    @Resource(name = "floorLevels")
    private Map<String, String> floorLevels;
    
    private DataConfig data;
    
    @Autowired
    public ExportService(final GroupService groupService, final DataService dataService, final MetricsService metricsService, final DataConfig data) {
        this.groupService = groupService;
        this.dataService = dataService;
        this.metricsService = metricsService;
        this.data = data;
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
    
    public void setDrillDown(Boolean drillDown) {
        this.drillDown = drillDown;
    }
    
    public void setMetrics(Boolean metrics) {
        this.metrics = metrics;
    }
    
    @PostConstruct
    public void init() {
        log.info("data: {}", data);
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
            selectResources(resources, areaDTO);
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
                getData(groupDTO, resourceName, resource, f, from, to);
            }
            
        }
        
        
    }
    
    private void getData(GroupDTO groupDTO, final String resourceName, final ResourceDTO resource, final File f, final Long fromTotal, final Long toTotal) {
        double diff = toTotal - fromTotal;
        final Calendar calendarFrom = Calendar.getInstance();
        calendarFrom.setTimeInMillis(fromTotal);
        final SortedMap<Long, DataPoint> dataPoints = new TreeMap<>();
        final Metrics currentMetrics = new Metrics();
        
        do {
            final long from = calendarFrom.getTimeInMillis();
            log.info("{}\t{}\t{}\t{}", groupDTO.getName(), resourceName, String.format("%.0f%%", ((from - fromTotal) / diff) * 100), calendarFrom.getTime());
            calendarFrom.add(Calendar.DAY_OF_YEAR, 3);
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
                if (cal.get(Calendar.DAY_OF_WEEK) == SUNDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    continue;
                }//exclude weekends
                if (cal.get(Calendar.HOUR_OF_DAY) < 8 || (cal.get(Calendar.HOUR_OF_DAY) == 13 && (cal.get(Calendar.MINUTE) > 30)) || (cal.get(Calendar.HOUR_OF_DAY) > 13)) {
                    continue;
                }//exclude off hours
                if (cal.get(Calendar.MONTH) == Calendar.DECEMBER && cal.get(Calendar.DAY_OF_MONTH) > 24) {
                    continue;
                }//exclude xmas
                if (cal.get(Calendar.MONTH) == Calendar.JANUARY && cal.get(Calendar.DAY_OF_MONTH) < 6) {
                    continue;
                }//exclude xmas
    
                final StringBuilder line = new StringBuilder();
                line.append(
                        cal.getTimeInMillis())
                        .append(",").append(cal.get(Calendar.YEAR))
                        .append(",").append((cal.get(Calendar.MONTH) + 1))
                        .append(",").append(cal.get(Calendar.DAY_OF_MONTH))
                        .append(",").append(cal.get(Calendar.HOUR_OF_DAY))
                        .append(",").append(cal.get(Calendar.MINUTE))
                        .append(",").append(String.format("%.2f", value.getValue()));
                if (metrics) {
                    boolean skip = metricsService.checkValue(resource, value.getValue(), currentMetrics, line);
                    if (skip) {
                        continue;
                    }
                    currentMetrics.incTotal();
                }
                line.append("\n");
                pw.append(line.toString());
                pw.flush();
            }
            double pAbnormalLow = (currentMetrics.getAbnormalLow() / currentMetrics.getTotal()) * 100;
            double pAbnormalHigh = (currentMetrics.getAbnormalHigh() / currentMetrics.getTotal()) * 100;
            double pAbnormal = ((currentMetrics.getAbnormalHigh() + currentMetrics.getAbnormalLow()) / currentMetrics.getTotal()) * 100;
            pw.append("rate,").append(String.valueOf(currentMetrics.getTotal()))
                    .append(",").append(String.valueOf(currentMetrics.getAbnormalLow() + currentMetrics.getAbnormalHigh()))
                    .append(",").append(String.valueOf(currentMetrics.getAbnormalLow()))
                    .append(",").append(String.valueOf(currentMetrics.getAbnormalHigh()))
                    .append(",").append(String.format("%.0f", pAbnormal))
                    .append(",").append(String.format("%.0f", pAbnormalLow))
                    .append(",").append(String.format("%.0f", pAbnormalHigh)).append("\n");
            log.info("{}\ttotal:{}\tabnormal:{}\t{}%\tabnormalLow:{}\t{}%\tabnormalHigh:{}\t{}%", groupDTO.getName(), currentMetrics.getTotal(),
                    (currentMetrics.getAbnormalLow() + currentMetrics.getAbnormalHigh()), String.format("%.1f", pAbnormal),
                    currentMetrics.getAbnormalLow(), String.format("%.1f", pAbnormalLow),
                    currentMetrics.getAbnormalHigh(), String.format("%.1f", pAbnormalHigh));
            
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
            String students = "0";
            if (buildingStudents.containsKey(buildingId)) {
                students = buildingStudents.get(buildingId);
            }
            fw.append("Building: ").append(buildingId).append("\n");
            fw.append("students: ").append(students).append("\n");
            fw.append("current: mA\n");
            fw.append("power: mWh\n");
            fw.append("temperature: C\n");
            fw.append("humidity: %RH\n");
            fw.append("luminosity: lux\n");
            fw.append("occupancy: 0 empty, 1 full, 0.xx intermediate\n");
            Collection<GroupDTO> subGroups = groupService.getSubGroups(groupDTO);
            for (GroupDTO subGroup : subGroups) {
                log.info("subGroup: {}", subGroup);
                Collection<GroupDTO> floorSubGroups = groupService.getSubGroups(subGroup);
                if (floorSubGroups.isEmpty()) {
                    createRoom(fw, subGroup, buildingId, null, areas);
                } else {
                    final String subAreaId = exportLastPathParam(subGroup);
                    fw.append("Floor: ").append(subAreaId).append("\n");
                    final String level;
                    if (floorLevels.containsKey(subAreaId)) {
                        level = floorLevels.get(subAreaId);
                    } else {
                        level = subGroup.getName().replaceAll("[^0-9]", "").trim();
                    }
                    fw.append("\t level: ").append(level).append("\n");
                    final String subGroupPath = "building" + buildingId + "/floor" + subAreaId;
                    if (drillDown) {
                        areas.put(subGroupPath, subGroup);
                    }
                    FileUtils.forceMkdir(new File(subGroupPath));
                    for (GroupDTO floorSubGroup : floorSubGroups) {
                        createRoom(fw, floorSubGroup, buildingId, subAreaId, areas);
                    }
                }
            }
            fw.close();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return areas;
    }
    
    private void createRoom(FileWriter fw, GroupDTO group, String buildingId, String subAreaId, Map<String, GroupDTO> areas) throws IOException {
        log.info("floorSubGroup: {}", group);
        final String floorSubAreaId = exportLastPathParam(group);
        fw.append("Room: ").append(floorSubAreaId).append("\n");
        String facing = "";
        if (roomFacing.containsKey(floorSubAreaId)) {
            facing = roomFacing.get(floorSubAreaId);
        }
        fw.append("\t facing: ").append(facing).append("\n");
        String usage;
        if (group.getName().contains("Πληροφορικής") || group.getName().contains("Η/Υ")) {
            usage = "ComputerRoom";
        } else if (group.getName().contains("Φυσικής")) {
            usage = "Lab";
        } else if (group.getName().contains("Αίθουσα")) {
            usage = "Classroom";
        } else {
            usage = group.getName();
        }
        fw.append("\t usage: ").append(usage).append("\n");
        final String floorSubGroupPath;
        if (subAreaId != null) {
            floorSubGroupPath = "building" + buildingId + "/floor" + subAreaId + "/room" + floorSubAreaId;
        } else {
            floorSubGroupPath = "building" + buildingId + "/room" + floorSubAreaId;
        }
        if (drillDown) {
            areas.put(floorSubGroupPath, group);
        }
        FileUtils.forceMkdir(new File(floorSubGroupPath));
    }
    
    private String exportLastPathParam(GroupDTO groupDTO) {
        return groupDTO.getPath().substring(groupDTO.getPath().lastIndexOf(".") + 1);
    }
    
    private void selectResources(final Map<String, ResourceDTO> resources, final GroupDTO areaDTO) {
        if (data.getTemperature()) {
            try {
                resources.put("temperature", groupService.getTemperatureResource(areaDTO));
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        if (data.getHumidity()) {
            try {
                resources.put("humidity", groupService.getRelativeHumidityResource(areaDTO));
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        if (data.getLuminosity()) {
            try {
                resources.put("luminosity", groupService.getLuminosityResource(areaDTO));
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        if (data.getNoise()) {
            try {
                resources.put("noise", groupService.getNoiseResource(areaDTO));
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        if (data.getCurrent()) {
            try {
                resources.put("current", groupService.getElectricalCurrentResource(areaDTO));
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        if (data.getPower()) {
            try {
                resources.put("power", groupService.getPowerConsumptionResource(areaDTO));
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
        if (data.getOccupancy()) {
            try {
                resources.put("occupancy", groupService.getOccupancyResource(areaDTO));
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
    }
}
