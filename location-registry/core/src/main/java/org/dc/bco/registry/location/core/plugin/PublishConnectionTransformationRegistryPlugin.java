/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;

public class PublishConnectionTransformationRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private Registry<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>, ?> registry;
    final Registry<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>, ?> locationRegistry;

    private TransformerFactory transformerFactory;
    private TransformPublisher transformPublisher;

    public PublishConnectionTransformationRegistryPlugin(final Registry<String, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder>, ?> locationRegistry) throws org.dc.jul.exception.InstantiationException {
        try {
            this.locationRegistry = locationRegistry;
            this.transformerFactory = TransformerFactory.getInstance();
            this.transformPublisher = transformerFactory.createTransformPublisher(PublishConnectionTransformationRegistryPlugin.class.getSimpleName());
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init(Registry<String, IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder>, ?> registry) throws InitializationException, InterruptedException {
        try {
            this.registry = registry;
            for (IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry : registry.getEntries()) {
                publishTransformation(entry);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void publishTransformation(IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry) {
        try {
            ConnectionConfig connectionConfig = entry.getMessage();

            if (connectionConfig.hasPlacementConfig() && connectionConfig.getPlacementConfig().hasPosition()) {

                if (!connectionConfig.hasId()) {
                    throw new NotAvailableException("connectionconfig.id");
                }

                if (!connectionConfig.hasPlacementConfig()) {
                    throw new NotAvailableException("connectionconfig.placement");
                }

                if (!connectionConfig.getPlacementConfig().hasPosition()) {
                    throw new NotAvailableException("connectionconfig.placement.position");
                }

                if (!connectionConfig.getPlacementConfig().hasTransformationFrameId() || connectionConfig.getPlacementConfig().getTransformationFrameId().isEmpty()) {
                    throw new NotAvailableException("connectionconfig.placement.transformationframeid");
                }

                if (!connectionConfig.getPlacementConfig().hasLocationId() || connectionConfig.getPlacementConfig().getLocationId().isEmpty()) {
                    throw new NotAvailableException("connectionconfig.placement.locationid");
                }

                logger.info("Publish " + locationRegistry.get(connectionConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId() + " to " + connectionConfig.getPlacementConfig().getTransformationFrameId());

                // Create the rct transform object with source and target frames
                Transform transformation = PoseTransformer.transform(connectionConfig.getPlacementConfig().getPosition(), locationRegistry.get(connectionConfig.getPlacementConfig().getLocationId()).getMessage().getPlacementConfig().getTransformationFrameId(), connectionConfig.getPlacementConfig().getTransformationFrameId());

                // Publish the transform object
                transformation.setAuthority(PublishConnectionTransformationRegistryPlugin.class.getSimpleName());
                transformPublisher.sendTransform(transformation, TransformType.STATIC);
            }
        } catch (CouldNotPerformException | TransformerException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not publish transformation of " + entry + "!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public void afterRegister(IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry) {
        publishTransformation(entry);
    }

    @Override
    public void afterUpdate(IdentifiableMessage<String, ConnectionConfig, ConnectionConfig.Builder> entry) throws CouldNotPerformException {
        publishTransformation(entry);
    }
}
