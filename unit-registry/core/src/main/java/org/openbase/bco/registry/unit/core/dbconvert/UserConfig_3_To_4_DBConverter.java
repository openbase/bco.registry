package org.openbase.bco.registry.unit.core.dbconvert;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.registry.lib.dbconvert.DescriptionBCO2DBConverter;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.storage.registry.version.DBVersionControl;

import java.io.File;
import java.util.Map;

/**
 * Converter which updates agents to the new label structure.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserConfig_3_To_4_DBConverter extends DescriptionBCO2DBConverter {

    public UserConfig_3_To_4_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }

    @Override
    public JsonObject upgrade(JsonObject outdatedDBEntry, Map<File, JsonObject> dbSnapshot) throws CouldNotPerformException {
        final JsonObject jsonObject = super.upgrade(outdatedDBEntry, dbSnapshot);
        final JsonObject user_config = jsonObject.getAsJsonObject("user_config");
        if (user_config.has("is_system_user")) {
            user_config.add("system_user", user_config.get("is_system_user"));
            user_config.remove("is_system_user");
        }
        return jsonObject;
    }
}
