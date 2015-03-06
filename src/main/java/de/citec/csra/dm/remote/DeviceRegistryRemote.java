/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm.remote;

import de.citec.csra.dm.registry.DeviceRegistryInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBRemoteService;
import java.util.concurrent.ExecutionException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceClassType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.registry.DeviceRegistryType.DeviceRegistry;

/**
 *
 * @author mpohling
 */
public class DeviceRegistryRemote extends RSBRemoteService<DeviceRegistry> implements DeviceRegistryInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceRegistry.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceClassType.DeviceClass.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DeviceConfigType.DeviceConfig.getDefaultInstance()));
    }

    @Override
    public void notifyUpdated(DeviceRegistry data) {

    }

    @Override
    public DeviceConfigType.DeviceConfig registerDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfigType.DeviceConfig) callMethodAsync("registerDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register device config!", ex);
        }
    }

    @Override
    public boolean containsDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (Boolean) callMethodAsync("containsDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device config!", ex);
        }
    }

    @Override
    public boolean containsDeviceConfigById(String deviceConfigId) throws CouldNotPerformException {
        try {
            return (Boolean) callMethodAsync("containsDeviceConfigById", deviceConfigId).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device config!", ex);
        }
    }

    @Override
    public DeviceConfigType.DeviceConfig updateDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfigType.DeviceConfig) callMethodAsync("updateDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not update device config!", ex);
        }
    }

    @Override
    public DeviceConfigType.DeviceConfig removeDeviceConfig(DeviceConfigType.DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {
            return (DeviceConfigType.DeviceConfig) callMethodAsync("removeDeviceConfig", deviceConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not remove device config!", ex);
        }
    }

    @Override
    public DeviceClassType.DeviceClass registerDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethodAsync("registerDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not register device class!", ex);
        }
    }

    @Override
    public boolean containsDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (Boolean) callMethodAsync("containsDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device class!", ex);
        }
    }

    @Override
    public boolean containsDeviceClassById(String deviceClassId) throws CouldNotPerformException {
        try {
            return (Boolean) callMethodAsync("containsDeviceClassById", deviceClassId).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not check device class!", ex);
        }
    }

    @Override
    public DeviceClassType.DeviceClass updateDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethodAsync("updateDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not update device class!", ex);
        }
    }

    @Override
    public DeviceClassType.DeviceClass removeDeviceClass(DeviceClassType.DeviceClass deviceClass) throws CouldNotPerformException {
        try {
            return (DeviceClassType.DeviceClass) callMethodAsync("removeDeviceClass", deviceClass).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not remove device class!", ex);
        }
    }
}
