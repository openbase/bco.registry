package org.openbase.bco.registry.unit.test;

/*-
 * #%L
 * BCO Registry Unit Test
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

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 * In this test the MockRegistry is started and all its locations except the root are
 * removed in a random order.
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class LocationRemovalTest extends AbstractBCORegistryTest {

    /**
     * In this test the MockRegistry is started and all its locations except the root are
     * removed in a random order.
     *
     * @throws Exception
     */
    @Test(timeout = 15000)
    public void removeAllLocationsTest() throws Exception {
        logger.info("RemoveAllLocationsTest");
        try {
            Random random = new Random();

            Registries.waitForData();
            while (true) {
                // get current locations, cannot be generated at the beginning because removing a tile can cause regions to be removed as well
                List<UnitConfig> locationConfigList = Registries.getLocationRegistry().getLocationConfigs();
                // remove root
                locationConfigList.remove(Registries.getLocationRegistry().getRootLocationConfig());

                // break if no more locations can be removed
                if (locationConfigList.isEmpty()) {
                    break;
                }

                // chose a location randomly
                UnitConfig locationConfig = locationConfigList.remove(random.nextInt(locationConfigList.size()));
                logger.info("Try to remove location[" + ScopeGenerator.generateStringRep(locationConfig.getScope()) + "].");

                // remove location
                Registries.getLocationRegistry().removeLocationConfig(locationConfig).get();

                // test if removal really worked
                assertFalse("LocationRegistry still contains locationConfig[" + locationConfig.getLabel() + "] after removal", Registries.getLocationRegistry().containsLocationConfig(locationConfig));
                logger.info("Removed location[" + ScopeGenerator.generateStringRep(locationConfig.getScope()) + "] successfully. " + locationConfigList.size() + " location[s] remaining.");
            }
        } catch (InterruptedException | ExecutionException | CouldNotPerformException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
    }
}
