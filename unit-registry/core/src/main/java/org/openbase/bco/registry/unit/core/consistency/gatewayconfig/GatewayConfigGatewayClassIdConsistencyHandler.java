package org.openbase.bco.registry.unit.core.consistency.gatewayconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.gateway.GatewayConfigType.GatewayConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class GatewayConfigGatewayClassIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig unitConfig = entry.getMessage();
        GatewayConfig gatewayConfig = unitConfig.getGatewayConfig();

        if (!gatewayConfig.hasGatewayClassId()) {
            throw new NotAvailableException("gatewayclass");
        }

        if (!gatewayConfig.hasGatewayClassId() || gatewayConfig.getGatewayClassId().isEmpty()) {
            throw new NotAvailableException("gatewayclass.id");
        }

        // get throws a CouldNotPerformException if the gateway class with the id does not exists
        CachedClassRegistryRemote.getRegistry().getGatewayClassById(gatewayConfig.getGatewayClassId());
    }
}
