/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.dm.core.DeviceManager;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerFactory;
import rst.geometry.PoseType;
import rst.geometry.RotationType;
import rst.geometry.TranslationType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public class DeviceTransformationConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, DeviceConfigType.DeviceConfig, DeviceConfig.Builder> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public DeviceTransformationConsistencyHandler() throws InstantiationException {
        try {
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(DeviceManager.APP_NAME);
        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public synchronized void processData(String id, IdentifiableMessage<String, DeviceConfig, DeviceConfig.Builder> entry, ProtoBufMessageMapInterface<String, DeviceConfig, DeviceConfig.Builder> entryMap, ProtoBufRegistryInterface<String, DeviceConfig, DeviceConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        DeviceConfig deviceConfig = entry.getMessage();

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
            transformation = transform(deviceConfig.getPlacementConfig().getPosition(), deviceConfig.getPlacementConfig().getLocationId(), deviceConfig.getId());

            try {
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            } catch (Exception ex) {
                ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex));
            }
        }

        // publish unit transformation
        for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {

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
                transformation = transform(unitConfig.getPlacementConfig().getPosition(), unitConfig.getPlacementConfig().getLocationId(), unitConfig.getId());

                try {
                    transformPublisher.sendTransform(transformation, TransformType.STATIC);
                } catch (Exception ex) {
                    ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex));
                }
            }
        }
    }

    //TODO mpohling: Should be moved into own jul transformer lib.
    public Transform transform(final PoseType.Pose position, String frameParent, String frameChild) {
        RotationType.Rotation pRotation = position.getRotation();
        TranslationType.Translation pTranslation = position.getTranslation();
        Quat4d jRotation = new Quat4d(pRotation.getQx(), pRotation.getQy(), pRotation.getQz(), pRotation.getQw());
        Vector3d jTranslation = new Vector3d(pTranslation.getX(), pTranslation.getY(), pTranslation.getZ());
        Transform3D transform3D = new Transform3D(jRotation, jTranslation, 1.0);
        Transform transform = new Transform(transform3D, frameParent, frameChild, System.currentTimeMillis());
        return transform;
    }

    /**
     * Check if given pose is neutral.
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
    public void reset() {
    }
}