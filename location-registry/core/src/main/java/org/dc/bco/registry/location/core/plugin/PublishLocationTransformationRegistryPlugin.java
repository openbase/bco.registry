package org.dc.bco.registry.location.core.plugin;

/*
 * #%L
 * REM LocationRegistry Core
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.bco.registry.location.core.LocationRegistryLauncher;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.protobuf.IdentifiableMessage;
import org.dc.jul.extension.rct.transform.PoseTransformer;
import org.dc.jul.storage.registry.Registry;
import org.dc.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformType;
import rct.TransformerException;
import rct.TransformerFactory;
import rst.spatial.LocationConfigType.LocationConfig;

public class PublishLocationTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    private Registry<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>, ?> registry;

    public PublishLocationTransformationRegistryPlugin() throws org.dc.jul.exception.InstantiationException {
        try {
            logger.debug("create location transformation publisher");
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(LocationRegistryLauncher.APP_NAME);
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(final Registry<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>, ?> registry) throws InitializationException, InterruptedException {
        try {
            this.registry = registry;
            for (IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry : registry.getEntries()) {
                publishtransformation(entry);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void afterRegister(final IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry) {
        publishtransformation(entry);
    }

    @Override
    public void afterUpdate(final IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry) throws CouldNotPerformException {
        publishtransformation(entry);
    }

    private void publishtransformation(final IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry) {
        try {
            LocationConfig locationConfig = entry.getMessage();

            // skip root locations
            if (locationConfig.getRoot()) {
                return;
            }

            if (!locationConfig.hasId()) {
                throw new NotAvailableException("locationconfig.id");
            }

            if (!locationConfig.hasPlacementConfig()) {
                throw new NotAvailableException("locationconfig.placementconfig");
            }

            if (!locationConfig.getPlacementConfig().hasPosition()) {
                throw new NotAvailableException("locationconfig.placementconfig.position");
            }

            if (!locationConfig.getPlacementConfig().hasTransformationFrameId() || locationConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                throw new NotAvailableException("locationconfig.placementconfig.transformationframeid");
            }

            if (!locationConfig.getPlacementConfig().hasLocationId() || locationConfig.getPlacementConfig().getLocationId().isEmpty()) {
                throw new NotAvailableException("locationconfig.placementconfig.locationid");
            }

            logger.info("Publish " + registry.get(locationConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId() + " to " + locationConfig.getPlacementConfig().getTransformationFrameId());

            // Create the rct transform object with source and target frames
            Transform transformation = PoseTransformer.transform(locationConfig.getPlacementConfig().getPosition(), registry.get(locationConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId(), locationConfig.getPlacementConfig().getTransformationFrameId());

            // Publish the transform object
            transformation.setAuthority(LocationRegistryLauncher.APP_NAME);
            transformPublisher.sendTransform(transformation, TransformType.STATIC);
        } catch (CouldNotPerformException | TransformerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "! RegistryConsistenct["+registry.isConsistent()+"]", ex), logger, LogLevel.WARN);
        }
    }
}
