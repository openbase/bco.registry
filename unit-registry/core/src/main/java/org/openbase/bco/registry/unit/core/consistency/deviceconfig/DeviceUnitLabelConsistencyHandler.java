package org.openbase.bco.registry.unit.core.consistency.deviceconfig;

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

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.unit.core.consistency.DefaultUnitLabelConsistencyHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceUnitLabelConsistencyHandler extends DefaultUnitLabelConsistencyHandler {

    private final Registry<String, IdentifiableMessage<String, DeviceClass, DeviceClass.Builder>> deviceClassRegistry;

    public DeviceUnitLabelConsistencyHandler() throws InstantiationException {
        super();
        try {
            this.deviceClassRegistry = CachedClassRegistryRemote.getRegistry().getDeviceClassRemoteRegistry();
        } catch (final CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * Generate the default label depending on the first label and the company of the device class.
     *
     * @param unitConfig the unit config for which a label is generated
     * @return company deviceClassFirstLabel firstAlias
     * @throws CouldNotPerformException if expected values are not set in the unit config or the device class is not available.
     */
    @Override
    public String generateDefaultLabel(final UnitConfig unitConfig) throws CouldNotPerformException {
        try {
            if (unitConfig == null) {
                throw new NotAvailableException("UnitConfig");
            }

            if (!unitConfig.hasDeviceConfig()) {
                throw new NotAvailableException("UnitConfig.DeviceConfig");
            }

            if (!unitConfig.getDeviceConfig().hasDeviceClassId() || unitConfig.getDeviceConfig().getDeviceClassId().isEmpty()) {
                throw new NotAvailableException("UnitConfig.DeviceConfig.DeviceClass");
            }

            final DeviceClass deviceClass = deviceClassRegistry.get(unitConfig.getDeviceConfig().getDeviceClassId()).getMessage();
            return deviceClass.getCompany() + " " + LabelProcessor.getBestMatch(deviceClass.getLabel()) + " " + super.generateDefaultLabel(unitConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate unit label!", ex);
        }
    }
}
