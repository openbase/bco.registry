package org.openbase.bco.registry.device.core.dbconvert;

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
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Map;
import org.openbase.jul.storage.registry.version.DBVersionConverter;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class DeviceConfig_1_To_2_DBConverter implements DBVersionConverter {

    @Override
    public JsonObject upgrade(JsonObject deviceConfig, final Map<File, JsonObject> dbSnapshot) {
        // remove the outdated owner(PersonType) from the inventory state
        JsonObject inventoryState = deviceConfig.get("inventory_state").getAsJsonObject();
        deviceConfig.remove("inventory_state");
        inventoryState.remove("owner");
        inventoryState.addProperty("owner_id", "");
        deviceConfig.add("inventory_state", inventoryState);

        return deviceConfig;
    }
}