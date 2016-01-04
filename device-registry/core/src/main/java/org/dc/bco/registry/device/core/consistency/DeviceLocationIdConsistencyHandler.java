/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.registry.device.core.consistency;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import org.dc.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.dc.jul.storage.registry.EntryModification;
import org.dc.jul.storage.registry.ProtoBufRegistryInterface;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import rst.homeautomation.device.DeviceConfigType;
import rst.spatial.PlacementConfigType;

/**
 *
 * @author mpohling
 */
public class DeviceLocationIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> {

    private final LocationRegistryRemote locationRegistryRemote;

    public DeviceLocationIdConsistencyHandler(final LocationRegistryRemote locationRegistryRemote) {
        this.locationRegistryRemote = locationRegistryRemote;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfigType.DeviceConfig.Builder deviceConfig = entry.getMessage().toBuilder();

        // check if placementconfig is available
        if (!deviceConfig.hasPlacementConfig()) {
            throw new NotAvailableException("device.placementconfig");
        }

        // setup base location of device has no location configured.
        if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
            deviceConfig.setPlacementConfig(PlacementConfigType.PlacementConfig.newBuilder(deviceConfig.getPlacementConfig()).setLocationId(locationRegistryRemote.getRootLocationConfig().getId()));
            throw new EntryModification(entry.setMessage(deviceConfig), this);
        }

        // verify if configured location exists.
        if (!locationRegistryRemote.containsLocationConfigById(deviceConfig.getPlacementConfig().getLocationId())) {
            throw new InvalidStateException("The configured Location[" + deviceConfig.getPlacementConfig().getLocationId() + "] of Device[" + deviceConfig.getId() + "] is unknown!");
        }
    }

    @Override
    public void reset() {
    }
}