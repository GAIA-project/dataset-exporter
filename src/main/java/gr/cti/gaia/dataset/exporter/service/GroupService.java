package gr.cti.gaia.dataset.exporter.service;

import net.sparkworks.cargo.client.GroupClient;
import net.sparkworks.cargo.common.dto.GroupDTO;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupService {
    
    private static final String TEMPERATURE_UUID = "905107a7-ca00-476f-b923-bf9720ed5c80";
    private static final String HUMIDITY_UUID = "795a7633-dd75-44eb-8ecb-11ecbf5a986b";
    private static final String LUMINOSITY_UUID = "484f9e6e-c3a5-4865-a03c-9ea5137a73ee";
    private static final String NOISE_UUID = "e9405ed3-01be-44f6-be39-e8415ff6b78b";
    private static final String ELECTRICAL_CURRENT_UUID = "80592482-2e98-410c-99b7-268796ff2c43";
    private static final String POWER_CONSUMPTION_UUID = "b749b7f2-9124-4a60-aa93-4771f69d9b9b";
    private static final String OCCUPANCY_MOTION_UUID = "16dc9b05-16e7-4854-93c2-aa31cb63cc8c";
    private static final String OCCUPANCY_MOVEMENT_UUID = "a254b151-960c-4297-a862-f991ca2e2c51";
    
    private final GroupClient groupClient;
    
    @Autowired
    public GroupService(final GroupClient groupClient) {
        this.groupClient = groupClient;
    }
    
    public GroupDTO findByUUID(final String uuid) {
        return groupClient.getByUUID(UUID.fromString(uuid));
    }
    
    public ResourceDTO getTemperatureResource(final GroupDTO groupDTO) {
        return getResourceByPhenomenonUuid(groupDTO, TEMPERATURE_UUID);
    }
    
    public ResourceDTO getRelativeHumidityResource(final GroupDTO groupDTO) {
        return getResourceByPhenomenonUuid(groupDTO, HUMIDITY_UUID);
    }
    
    public ResourceDTO getLuminosityResource(final GroupDTO groupDTO) {
        return getResourceByPhenomenonUuid(groupDTO, LUMINOSITY_UUID);
    }

    public ResourceDTO getNoiseResource(final GroupDTO groupDTO) {
        return getResourceByPhenomenonUuid(groupDTO, NOISE_UUID);
    }
    
    public ResourceDTO getElectricalCurrentResource(final GroupDTO groupDTO) {
        return getResourceByPhenomenonUuid(groupDTO, ELECTRICAL_CURRENT_UUID);
    }
    
    public ResourceDTO getPowerConsumptionResource(final GroupDTO groupDTO) {
        return getResourceByPhenomenonUuid(groupDTO, POWER_CONSUMPTION_UUID);
    }
    
    
    public ResourceDTO getOccupancyResource(final GroupDTO groupDTO) {
        final ResourceDTO res1 = getResourceByPhenomenonUuid(groupDTO, OCCUPANCY_MOTION_UUID);
        return res1 != null ? res1 : getResourceByPhenomenonUuid(groupDTO, OCCUPANCY_MOVEMENT_UUID);
    }
    
    private ResourceDTO getResourceByPhenomenonUuid(final GroupDTO groupDTO, final String phenomenonUUID) {
        final Iterator<ResourceDTO> resources = groupClient.getGroupResources(groupDTO.getUuid()).stream().filter(resourceDTO -> resourceDTO.getGroupUuid().equals(groupDTO.getUuid()) && resourceDTO.getPhenomenonUuid()!=null && resourceDTO.getPhenomenonUuid().toString().equals(phenomenonUUID)).distinct().collect(Collectors.toList()).iterator();
        return resources.hasNext() ? resources.next() : null;
    }
    
    public Collection<GroupDTO> getSubGroups(final GroupDTO groupDTO) {
        return groupClient.getSubGroups(groupDTO.getUuid(), 1);
    }
}
