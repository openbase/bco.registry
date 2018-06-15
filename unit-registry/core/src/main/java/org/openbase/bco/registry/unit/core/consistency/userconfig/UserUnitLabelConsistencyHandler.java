package org.openbase.bco.registry.unit.core.consistency.userconfig;

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

import org.openbase.bco.registry.unit.core.consistency.DefaultUnitLabelConsistencyHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.user.UserConfigType.UserConfig;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserUnitLabelConsistencyHandler extends DefaultUnitLabelConsistencyHandler {

    /**
     * Generate a default user name.
     *
     * @param unitConfig the unit config for which a label is generated
     * @return username (firstName lastName)
     * @throws CouldNotPerformException if values in the user config are missing
     */
    @Override
    protected String generateDefaultLabel(UnitConfig unitConfig) throws CouldNotPerformException {
        final UserConfig userConfig = unitConfig.getUserConfig();

        if (!userConfig.hasFirstName() || userConfig.getFirstName().isEmpty()) {
            throw new NotAvailableException("UserConfig.FirstName");
        }

        if (!userConfig.hasLastName() || userConfig.getLastName().isEmpty()) {
            throw new NotAvailableException("UserConfig.LastName");
        }

        if (!userConfig.hasUserName() || userConfig.getUserName().isEmpty()) {
            throw new NotAvailableException("UserConfig.FirstName");
        }

        return userConfig.getUserName() + " (" + userConfig.getFirstName() + " " + userConfig.getLastName() + ")";
    }

    /**
     * Return the label to make sure user label are unique.
     *
     * @param label      the label for which the key is generated
     * @param unitConfig the unit having the label
     * @return the label
     */
    @Override
    protected String generateKey(String label, UnitConfig unitConfig) {
        return label;
    }
}