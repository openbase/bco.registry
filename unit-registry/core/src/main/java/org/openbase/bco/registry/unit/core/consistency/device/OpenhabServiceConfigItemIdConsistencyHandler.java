package org.openbase.bco.registry.unit.core.consistency.device;

/*
 * #%L
 * REM DeviceRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.MetaConfigProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.configuration.MetaConfigType.MetaConfig;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceRegistryDataType.DeviceRegistryData;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitRegistryDataType.UnitRegistryData;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class OpenhabServiceConfigItemIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";
    public static final String OPENHAB_BINDING_ITEM_ID = "OPENHAB_BINDING_ITEM_ID";

    private final ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> deviceClassRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry;

    public OpenhabServiceConfigItemIdConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, DeviceClass, DeviceClass.Builder, DeviceRegistryData.Builder> deviceClassRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> locationRegistry,
            final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> dalUnitRegistry) {
        this.deviceClassRegistry = deviceClassRegistry;
        this.locationRegistry = locationRegistry;
        this.dalUnitRegistry = dalUnitRegistry;
    }

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig deviceUnitConfig = entry.getMessage();

        for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
            boolean modification = false;
            UnitConfig.Builder unitConfig = dalUnitRegistry.getMessage(unitId).toBuilder();
            UnitConfig.Builder unitConfigClone = unitConfig.clone();
            unitConfig.clearServiceConfig();
            for (ServiceConfig.Builder serviceConfig : unitConfigClone.getServiceConfigBuilderList()) {

                if (!serviceConfig.hasBindingConfig()) {
                    continue;
                }

                if (serviceConfig.getBindingConfig().getBindingId().equals("OPENHAB")) {
                    String itemId = generateItemName(entry.getMessage(), deviceClassRegistry.getMessage(deviceUnitConfig.getDeviceConfig().getDeviceClassId()).getLabel(), unitConfig.clone().build(), serviceConfig.clone().build(), locationRegistry.getMessage(unitConfig.getPlacementConfig().getLocationId()));

                    MetaConfig metaConfig;

                    // check if meta config already exist, otherwise create one.
                    if (!serviceConfig.getBindingConfig().hasMetaConfig()) {
                        serviceConfig.setBindingConfig(serviceConfig.getBindingConfig().toBuilder().setMetaConfig(MetaConfig.getDefaultInstance()));
                        modification = true;
                    }

                    metaConfig = serviceConfig.getBindingConfig().getMetaConfig();

                    String configuredItemId = "";
                    try {
                        configuredItemId = MetaConfigProcessor.getValue(metaConfig, OPENHAB_BINDING_ITEM_ID);
                    } catch (NotAvailableException ex) {
                    }

                    if (!configuredItemId.equals(itemId)) {
                        metaConfig = MetaConfigProcessor.setValue(metaConfig, OPENHAB_BINDING_ITEM_ID, itemId);
                        serviceConfig.setBindingConfig(serviceConfig.getBindingConfig().toBuilder().setMetaConfig(metaConfig));
                        modification = true;
                    }
                }
                unitConfig.addServiceConfig(serviceConfig);
            }
            if (modification) {
                dalUnitRegistry.update(unitConfig.build());
            }
        }
    }

    public static String generateItemName(final UnitConfig device, final String deviceClassLabel, final UnitConfig unit, final ServiceConfig service, final UnitConfig location) throws CouldNotPerformException {
        if (device == null) {
            throw new NotAvailableException("deviceconfig");
        }

        if (unit == null) {
            throw new NotAvailableException("unitconfig");
        }

        if (service == null) {
            throw new NotAvailableException("serviceconfig");
        }

        return StringProcessor.transformToIdString(deviceClassLabel)
                + ITEM_SEGMENT_DELIMITER
                + ScopeGenerator.generateStringRepWithDelimiter(location.getScope(), ITEM_SUBSEGMENT_DELIMITER)
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(unit.getType().toString())
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformToIdString(unit.getLabel())
                + ITEM_SEGMENT_DELIMITER
                + StringProcessor.transformUpperCaseToCamelCase(service.getServiceTemplate().getType().toString());
    }
}