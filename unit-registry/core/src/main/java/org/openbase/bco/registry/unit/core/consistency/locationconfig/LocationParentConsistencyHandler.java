package org.openbase.bco.registry.unit.core.consistency.locationconfig;

/*
 * #%L
 * REM LocationRegistry Core
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
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;

/**
 *
 @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationParentConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitConfig.Builder unitLocationConfig = entry.getMessage().toBuilder();

        // check if placement exists.
        if (!unitLocationConfig.hasPlacementConfig()) {
            throw new NotAvailableException("locationconfig.placementconfig");
        }

        // check children consistency
        if (!unitLocationConfig.getPlacementConfig().hasLocationId()) {
            throw new NotAvailableException("locationconfig.placementconfig.locationid");
        }

        // skip root locations
        if (unitLocationConfig.getLocationConfig().getRoot()) {
            return;
        }

        // check if parent is registered.
        if (!entryMap.containsKey(unitLocationConfig.getPlacementConfig().getLocationId())) {
            entry.setMessage(unitLocationConfig.setPlacementConfig(unitLocationConfig.getPlacementConfig().toBuilder().clearLocationId()));
            throw new EntryModification("Parent[" + unitLocationConfig.getPlacementConfig().getLocationId() + "] of child[" + unitLocationConfig.getId() + "] is unknown! Entry will moved to root location!", entry, this);
        }

        // check if parents knows given child.
        IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> parent = registry.get(unitLocationConfig.getPlacementConfig().getLocationId());
        if (parent != null && !parentHasChild(parent.getMessage(), unitLocationConfig.build()) && !parent.getMessage().getPlacementConfig().getLocationId().equals(unitLocationConfig.getId())) {
            LocationConfig.Builder parentLocationConfig = parent.getMessage().getLocationConfig().toBuilder().addChildId(unitLocationConfig.getId());
            parent.setMessage(parent.getMessage().toBuilder().setLocationConfig(parentLocationConfig));
            throw new EntryModification("Parent[" + parent.getId() + "] does not know Child[" + unitLocationConfig.getId() + "]", parent, this);
        }
    }

    private boolean parentHasChild(UnitConfig parent, UnitConfig child) {
        return parent.getLocationConfig().getChildIdList().stream().anyMatch((children) -> (children.equals(child.getId())));
    }
}