package org.openbase.bco.registry.unit.core.consistency.dalunitconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.HashMap;
import java.util.Map;

import org.openbase.bco.registry.app.remote.CachedAppRegistryRemote;
import org.openbase.bco.registry.device.remote.CachedDeviceRegistryRemote;
import org.openbase.bco.registry.lib.util.DeviceConfigUtils;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DalUnitLabelConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry;
    private final Map<String, String> oldUnitHostLabelMap;

    public DalUnitLabelConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> deviceRegistry, final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> appRegistry) throws InstantiationException {
        try {
            this.deviceClassRegistry = CachedDeviceRegistryRemote.getRegistry().getDeviceClassRemoteRegistry();
            this.deviceRegistry = deviceRegistry;
            this.appRegistry = appRegistry;
            this.oldUnitHostLabelMap = new HashMap<>();
        } catch (final CouldNotPerformException | InterruptedException ex) {
            // remove InterruptedException in bco 2.0 release
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder dalUnitConfig = entry.getMessage().toBuilder();

        // filter virtual units
        if (UnitConfigProcessor.isVirtualUnit(dalUnitConfig)) {
            return;
        }

        if (!dalUnitConfig.hasUnitHostId() || dalUnitConfig.getUnitHostId().isEmpty()) {
            throw new NotAvailableException("unitConfig.unitHostId");
        }

        // handle if host unit is a device
        if (deviceRegistry.contains(dalUnitConfig.getUnitHostId())) {
            UnitConfig hostUnitConfig = deviceRegistry.getMessage(dalUnitConfig.getUnitHostId());
            DeviceClass deviceClass = deviceClassRegistry.get(hostUnitConfig.getDeviceConfig().getDeviceClassId()).getMessage();

            if (!oldUnitHostLabelMap.containsKey(dalUnitConfig.getId())) {
                oldUnitHostLabelMap.put(dalUnitConfig.getId(), hostUnitConfig.getLabel());
            }

            boolean hasDuplicatedUnitType = DeviceConfigUtils.checkDuplicatedUnitType(hostUnitConfig, deviceClass, registry);

            // Setup device label if unit has no label configured.
            if (!dalUnitConfig.hasLabel() || dalUnitConfig.getLabel().isEmpty()) {
                if (DeviceConfigUtils.setupUnitLabelByDeviceConfig(dalUnitConfig, hostUnitConfig, deviceClass, hasDuplicatedUnitType)) {
                    throw new EntryModification(entry.setMessage(dalUnitConfig), this);
                }
            }

            String oldLabel = oldUnitHostLabelMap.get(dalUnitConfig.getId());
            if (!oldLabel.equals(hostUnitConfig.getLabel())) {
                oldUnitHostLabelMap.put(dalUnitConfig.getId(), hostUnitConfig.getLabel());
                if (dalUnitConfig.getLabel().equals(oldLabel)) {
                    if (DeviceConfigUtils.setupUnitLabelByDeviceConfig(dalUnitConfig, hostUnitConfig, deviceClass, hasDuplicatedUnitType)) {
                        throw new EntryModification(entry.setMessage(dalUnitConfig), this);
                    }
                }
            }
            // handle if host unit is a app
        } else if (appRegistry.contains(dalUnitConfig.getUnitHostId())) {
            UnitConfig hostUnitConfig = appRegistry.getMessage(dalUnitConfig.getUnitHostId());

            if (!oldUnitHostLabelMap.containsKey(dalUnitConfig.getId())) {
                oldUnitHostLabelMap.put(dalUnitConfig.getId(), hostUnitConfig.getLabel());
            }

            // Setup alias as label if unit has no label configured.
            if (!dalUnitConfig.hasLabel() || dalUnitConfig.getLabel().isEmpty()) {
                if (dalUnitConfig.getAliasCount() <= 1) {
                    throw new InvalidStateException("Alias not provided by Unit[" + dalUnitConfig.getId() + "]!");
                }
                throw new EntryModification(entry.setMessage(dalUnitConfig.setLabel(dalUnitConfig.getAlias(0))), this);
            }
        }
    }
}
