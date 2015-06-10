/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.remote;

import de.citec.lm.lib.generator.LocationIDGenerator;
import de.citec.jul.storage.registry.RemoteRegistry;
import de.citec.lm.lib.registry.LocationRegistryInterface;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jp.JPLocationRegistryScope;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.extension.rsb.scope.ScopeProvider;
import java.util.ArrayList;
import java.util.List;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.spatial.LocationConfigType;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import rst.homeautomation.unit.UnitTemplateType;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 *
 * @author mpohling
 */
public class LocationRegistryRemote extends RSBRemoteService<LocationRegistry> implements LocationRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(LocationConfig.getDefaultInstance()));
    }

    private final RemoteRegistry<String, LocationConfig, LocationConfig.Builder, LocationRegistry.Builder> locationRemoteRegistry;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public LocationRegistryRemote() throws InstantiationException {
        try {
            this.locationRemoteRegistry = new RemoteRegistry<>(new LocationIDGenerator());
            deviceRegistryRemote = new DeviceRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(String label, ScopeProvider location) throws InitializationException {
        deviceRegistryRemote.init();
        super.init(label, location);
    }

    @Override
    public synchronized void init(Scope scope) throws InitializationException {
        deviceRegistryRemote.init();
        super.init(scope);
    }

    public void init() throws InitializationException {
        this.init(JPService.getProperty(JPLocationRegistryScope.class).getValue());
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        deviceRegistryRemote.activate();
        super.activate();
        try {
            notifyUpdated(requestStatus());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(logger, new CouldNotPerformException("Initial registry sync failed!", ex));
        }
    }

    @Override
    public void shutdown() {
        deviceRegistryRemote.shutdown();
        super.shutdown();
    }

    @Override
    public void notifyUpdated(final LocationRegistry data) throws CouldNotPerformException {
        locationRemoteRegistry.notifyRegistryUpdated(data.getLocationConfigList());
    }

    @Override
    public LocationConfigType.LocationConfig registerLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        try {
            return (LocationConfigType.LocationConfig) callMethod("registerLocationConfig", locationConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register location config!", ex);
        }
    }

    @Override
    public LocationConfig getLocationConfigById(final String locationConfigId) throws CouldNotPerformException {
        getData();
        return locationRemoteRegistry.getMessage(locationConfigId);
    }

    @Override
    public Boolean containsLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        getData();
        return locationRemoteRegistry.contains(locationConfig);
    }

    @Override
    public Boolean containsLocationConfigById(final String locationConfigId) throws CouldNotPerformException {
        getData();
        return locationRemoteRegistry.contains(locationConfigId);
    }

    @Override
    public LocationConfig updateLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        try {
            return (LocationConfigType.LocationConfig) callMethod("updateLocationConfig", locationConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update location[" + locationConfig + "]!", ex);
        }
    }

    @Override
    public LocationConfig removeLocationConfig(final LocationConfig locationConfig) throws CouldNotPerformException {
        try {
            return (LocationConfigType.LocationConfig) callMethod("removeLocationConfig", locationConfig);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not remove location[" + locationConfig + "]!", ex);
        }
    }

    @Override
    public List<LocationConfig> getLocationConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<LocationConfig> messages = locationRemoteRegistry.getMessages();
        return messages;
    }

    @Override
    public List<UnitConfigType.UnitConfig> getUnitConfigs(final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfigType.UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getLocationConfigById(locationConfigId).getUnitIdList()) {
            unitConfigList.add(deviceRegistryRemote.getUnitConfigById(unitConfigId));
        }
        return unitConfigList;
    }

    public List<UnitConfigType.UnitConfig> getUnitConfigs(final UnitTemplateType.UnitTemplate.UnitType type, final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<UnitConfigType.UnitConfig> unitConfigList = new ArrayList<>();
        for (String unitConfigId : getLocationConfigById(locationConfigId).getUnitIdList()) {
            UnitConfigType.UnitConfig unitConfig = deviceRegistryRemote.getUnitConfigById(unitConfigId);
            if (unitConfig.getTemplate().getType().equals(type)) {
                unitConfigList.add(unitConfig);
            }
        }
        return unitConfigList;
    }

    @Override
    public List<ServiceConfigType.ServiceConfig> getServiceConfigs(final String locationConfigId) throws CouldNotPerformException, NotAvailableException {
        List<ServiceConfigType.ServiceConfig> serviceConfigList = new ArrayList<>();
        for (UnitConfigType.UnitConfig unitConfig : getUnitConfigs(locationConfigId)) {
            serviceConfigList.addAll(unitConfig.getServiceConfigList());
        }
        return serviceConfigList;
    }

    public List<LocationConfig> getRootLocationConfigs() throws CouldNotPerformException, NotAvailableException {
        getData();
        List<LocationConfig> rootLocationConfigs = new ArrayList<>();
        for (LocationConfig locationConfig : locationRemoteRegistry.getMessages()) {
            if (locationConfig.hasRoot()) {
                rootLocationConfigs.add(locationConfig);
            }
        }
        return rootLocationConfigs;
    }
}