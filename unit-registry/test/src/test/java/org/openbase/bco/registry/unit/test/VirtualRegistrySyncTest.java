package org.openbase.bco.registry.unit.test;

/*
 * #%L
 * BCO Registry Unit Test
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openbase.bco.registry.agent.core.AgentRegistryController;
import org.openbase.bco.registry.app.core.AppRegistryController;
import org.openbase.bco.registry.device.core.DeviceRegistryController;
import org.openbase.bco.registry.location.core.LocationRegistryController;
import org.openbase.bco.registry.unit.core.UnitRegistryController;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.Stopwatch;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.DeviceRegistryDataType.DeviceRegistryData;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;
import rst.domotic.state.InventoryStateType.InventoryState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;
import rst.domotic.unit.device.DeviceConfigType.DeviceConfig;
import rst.domotic.unit.location.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class VirtualRegistrySyncTest {

    private static final Logger logger = LoggerFactory.getLogger(VirtualRegistrySyncTest.class);

    private static final String ROOT_LOCATION_LABEL = "syncTestRoot";
    private static UnitConfig ROOT_LOCATION;

    private static final String DEVICE_CLASS_LABEL = "syncTestDeviceClass";
    private static final String DEVICE_CLASS_COMPANY = "syncTestCompany";
    private static final String DEVICE_CLASS_PRODUCT_NUMBER = "12344321";
    private static DeviceClass DEVICE_CLASS;

    private static DeviceRegistryController deviceRegistry;
    private static UnitRegistryController unitRegistry;
    private static AppRegistryController appRegistry;
    private static AgentRegistryController agentRegistry;

    private static LocationRegistryController locationRegistry;

    @BeforeClass
    public static void setUpClass() throws InstantiationException, InitializationException, IOException, InvalidStateException, JPServiceException, InterruptedException, CouldNotPerformException, ExecutionException {
        JPService.setupJUnitTestMode();

        deviceRegistry = new DeviceRegistryController();
        unitRegistry = new UnitRegistryController();
        appRegistry = new AppRegistryController();
        agentRegistry = new AgentRegistryController();
        locationRegistry = new LocationRegistryController();

        deviceRegistry.init();
        unitRegistry.init();
        appRegistry.init();
        agentRegistry.init();
        locationRegistry.init();

        Thread deviceRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    deviceRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread unitRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    unitRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread appRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    appRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread agentRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    agentRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        Thread locationRegistryThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    locationRegistry.activate();
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });

        deviceRegistryThread.start();
        unitRegistryThread.start();
        appRegistryThread.start();
        agentRegistryThread.start();
        locationRegistryThread.start();

        deviceRegistryThread.join();
        unitRegistryThread.join();
        appRegistryThread.join();
        agentRegistryThread.join();
        locationRegistryThread.join();

        LocationConfig rootLocationConfig = LocationConfig.newBuilder().setRoot(true).build();
        ROOT_LOCATION = locationRegistry.registerLocationConfig(UnitConfig.newBuilder().setLabel(ROOT_LOCATION_LABEL).setType(UnitType.LOCATION).setLocationConfig(rootLocationConfig).build()).get();
        DEVICE_CLASS = deviceRegistry.registerDeviceClass(DeviceClass.newBuilder().setLabel(DEVICE_CLASS_LABEL).setCompany(DEVICE_CLASS_COMPANY).setProductNumber(DEVICE_CLASS_PRODUCT_NUMBER).build()).get();

        while (!unitRegistry.getDeviceRegistryRemote().containsDeviceClass(DEVICE_CLASS)) {
            Thread.sleep(50);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        if (unitRegistry != null) {
            unitRegistry.shutdown();
        }
        if (deviceRegistry != null) {
            deviceRegistry.shutdown();
        }
        if (appRegistry != null) {
            appRegistry.shutdown();
        }
        if (agentRegistry != null) {
            agentRegistry.shutdown();
        }
        if (locationRegistry != null) {
            locationRegistry.shutdown();
        }
    }

    private DeviceConfig deviceConfig = DeviceConfig.getDefaultInstance();
    private UnitConfig deviceUnitConfig = UnitConfig.getDefaultInstance();

    private final Stopwatch deviceStopWatch = new Stopwatch();
    private final Stopwatch locationStopWatch = new Stopwatch();

    private final SyncObject DEVICE_LOCK = new SyncObject("deviceRegistryLock");
    private final SyncObject LOCATION_LOCK = new SyncObject("locationRegistryLock");

    @Test(timeout = 5000)
    public void testVirtualRegistrySync() throws Exception {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        PlacementConfig rootPlacement = PlacementConfig.newBuilder().setLocationId(ROOT_LOCATION.getId()).build();
        InventoryState inventoryState = InventoryState.newBuilder().setValue(InventoryState.State.INSTALLED).build();

        String label = "syncTestDevice - ";
        String serialNumber = "0000-";

        final Observer deviceRegistryObserver = (Observer<DeviceRegistryData>) (Observable<DeviceRegistryData> source, DeviceRegistryData data) -> {
            synchronized (DEVICE_LOCK) {
                DEVICE_LOCK.notifyAll();
            }
        };
        deviceRegistry.addDataObserver(deviceRegistryObserver);
        Thread waitForDeviceUpdateThread;

        final Observer locationRegistryObserver = (Observer<LocationRegistryData>) (Observable<LocationRegistryData> source, LocationRegistryData data) -> {
            synchronized (LOCATION_LOCK) {
                LOCATION_LOCK.notifyAll();
            }
        };
        locationRegistry.addDataObserver(locationRegistryObserver);
        Thread waitForLocationUpdateThread;

        for (int i = 0; i < 10; ++i) {
            waitForDeviceUpdateThread = getDeviceThread();
            waitForLocationUpdateThread = getLocationThread();

            deviceConfig = DeviceConfig.newBuilder().setDeviceClassId(DEVICE_CLASS.getId()).setSerialNumber(serialNumber + i).setInventoryState(inventoryState).build();
            deviceUnitConfig = UnitConfig.newBuilder().setType(UnitType.DEVICE).setLabel(label + i).setPlacementConfig(rootPlacement).setDeviceConfig(deviceConfig).build();

            waitForDeviceUpdateThread.start();
            waitForLocationUpdateThread.start();

            deviceStopWatch.restart();
            locationStopWatch.restart();

            try {
                if ((i % 2) == 0) {
                    deviceUnitConfig = unitRegistry.registerUnitConfig(deviceUnitConfig).get();
                } else {
                    deviceUnitConfig = deviceRegistry.registerDeviceConfig(deviceUnitConfig).get();
                }
            } catch (ExecutionException ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
            }

            waitForDeviceUpdateThread.join();
            waitForLocationUpdateThread.join();

            logger.info(deviceStopWatch.getTime() + " ms until device[" + deviceUnitConfig.getLabel() + "] registered in deviceRegistry!");
            logger.info(locationStopWatch.getTime() + " ms until device[" + deviceUnitConfig.getLabel() + "] registered in root location!");
        }

        deviceRegistry.removeDataObserver(deviceRegistryObserver);
        locationRegistry.removeDataObserver(locationRegistryObserver);
    }

    private Thread getDeviceThread() {
        return new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (DEVICE_LOCK) {

                    try {
                        while (!deviceUnitConfig.hasId() && !containsByLabel(new ArrayList<>(deviceRegistry.getDeviceConfigs()))) {
                            DEVICE_LOCK.wait();
                        }
                        deviceStopWatch.stop();
                    } catch (CouldNotPerformException | InterruptedException ex) {
                        ExceptionPrinter.printHistory(ex, logger);
                    }
                }
            }
        });
    }

    private boolean containsByLabel(final List<UnitConfig> deviceUnitConfigList) {
        return deviceUnitConfigList.stream().anyMatch((unitConfig) -> (unitConfig.getLabel().equals(deviceUnitConfig.getLabel())));
    }

    private Thread getLocationThread() {
        return new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    List<UnitConfig> deviceUnitConfigList = new ArrayList<>();
                    for (String id : locationRegistry.getRootLocationConfig().getLocationConfig().getUnitIdList()) {
                        deviceUnitConfigList.add(unitRegistry.getUnitConfigById(id));
                    }
                    synchronized (LOCATION_LOCK) {
                        try {
                            while (!containsByLabel(deviceUnitConfigList)) {
                                LOCATION_LOCK.wait();
                                deviceUnitConfigList.clear();
                                for (String id : locationRegistry.getRootLocationConfig().getLocationConfig().getUnitIdList()) {
                                    deviceUnitConfigList.add(unitRegistry.getUnitConfigById(id));
                                }
                            }
                            locationStopWatch.stop();
                        } catch (CouldNotPerformException | InterruptedException ex) {
                            ExceptionPrinter.printHistory(ex, logger);
                        }
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(ex, logger);
                }
            }
        });
    }
}