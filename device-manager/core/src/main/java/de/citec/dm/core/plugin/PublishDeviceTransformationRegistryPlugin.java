/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.plugin;

import de.citec.dm.core.DeviceManager;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.rct.transform.PoseTransformer;
import de.citec.jul.storage.registry.RegistryInterface;
import de.citec.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerFactory;
import rst.geometry.PoseType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType;

public class PublishDeviceTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishDeviceTransformationRegistryPlugin() throws de.citec.jul.exception.InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(DeviceManager.APP_NAME);
        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(RegistryInterface<String, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder>, ?> registry) throws CouldNotPerformException {
        for (IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry : registry.getEntries()) {
            publishtransformation(entry);
        }
    }

    public void publishtransformation(IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry) {
        try {
            DeviceConfigType.DeviceConfig deviceConfig = entry.getMessage();

            if (!deviceConfig.hasId()) {
                throw new NotAvailableException("deviceconfig.id");
            }

            if (!deviceConfig.hasPlacementConfig()) {
                throw new NotAvailableException("deviceconfig.placement");
            }

            if (!deviceConfig.getPlacementConfig().hasPosition()) {
                throw new NotAvailableException("deviceconfig.placement.position");
            }

            if (!deviceConfig.getPlacementConfig().hasLocationId() || deviceConfig.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("unitconfig.placement.locationid");
            }

            Transform transformation;

            // publish device transformation
            if (isTransformationPresent(deviceConfig.getPlacementConfig().getPosition())) {
                logger.info("Publish " + deviceConfig.getPlacementConfig().getLocationId() + " to " + deviceConfig.getId());
                transformation = PoseTransformer.transform(deviceConfig.getPlacementConfig().getPosition(), deviceConfig.getPlacementConfig().getLocationId(), deviceConfig.getId());

                try {
                    transformPublisher.sendTransform(transformation, TransformType.STATIC);
                } catch (Exception ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
                }
            }

            // publish unit transformation
            for (UnitConfigType.UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {

                if (!unitConfig.hasPlacementConfig()) {
                    throw new NotAvailableException("unitconfig.placement");
                }

                if (!unitConfig.getPlacementConfig().hasPosition()) {
                    throw new NotAvailableException("unitconfig.placement.position");
                }

                if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                    throw new NotAvailableException("unitconfig.placement.locationid");
                }

                if (isTransformationPresent(unitConfig.getPlacementConfig().getPosition())) {
                    transformation = PoseTransformer.transform(unitConfig.getPlacementConfig().getPosition(), unitConfig.getPlacementConfig().getLocationId(), unitConfig.getId());

                    try {
                        transformPublisher.sendTransform(transformation, TransformType.STATIC);
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
                    }
                }
            }
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish device transformation of " + entry + "!", ex), logger, LogLevel.ERROR);
        }
    }

    /**
     * Check if given pose is neutral.
     *
     * @param position
     * @return
     */
    private boolean isTransformationPresent(final PoseType.Pose position) {
        if (!position.hasRotation() && !position.hasTranslation()) {
            return false;
        }

        return !(position.getTranslation().getX() == 0.0
                && position.getTranslation().getY() == 0.0
                && position.getTranslation().getZ() == 0.0
                && position.getRotation().getQw() == 1.0
                && position.getRotation().getQx() == 0.0
                && position.getRotation().getQy() == 0.0
                && position.getRotation().getQx() == 0.0);
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry) {
        publishtransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, DeviceConfigType.DeviceConfig, DeviceConfigType.DeviceConfig.Builder> entry) throws CouldNotPerformException {
        publishtransformation(entry);
    }
}
