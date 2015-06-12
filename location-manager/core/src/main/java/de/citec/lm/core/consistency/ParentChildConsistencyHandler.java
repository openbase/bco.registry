/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.lm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.rsb.container.IdentifiableMessage;
import de.citec.jul.extension.rsb.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class ParentChildConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, LocationConfig, LocationConfig.Builder> {

    private static final Logger logger = LoggerFactory.getLogger(ParentChildConsistencyHandler.class);

    public void processData(String id, IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> entry, ProtoBufMessageMapInterface<String, LocationConfig, LocationConfig.Builder> entryMap, ProtoBufRegistryInterface<String, LocationConfig, LocationConfig.Builder> registry) throws CouldNotPerformException, EntryModification {

        LocationConfig locationConfig = entry.getMessage();

        // check if parents know their children.
        if (locationConfig.hasParentId()) {

            // check if parent is registered.
            if (!entryMap.containsKey(locationConfig.getParentId())) {
                logger.warn("Parent[" + locationConfig.getParentId() + "] of child[" + locationConfig.getId() + "] is unknown! Parent entry will be erased!");
                entry.setMessage(locationConfig.toBuilder().clearParentId().setRoot(true).build());
                throw new EntryModification(entry, this);
            }

            // check if parents knows given child.
            IdentifiableMessage<String, LocationConfigType.LocationConfig, LocationConfig.Builder> parent = registry.get(locationConfig.getParentId());
            if (parent != null && !parentHasChild(parent.getMessage(), locationConfig)) {
                parent.setMessage(parent.getMessage().toBuilder().addChild(locationConfig).build());
                throw new EntryModification(entry, this);
            }
        }

        try {
            // check if children know their parent.
            for (LocationConfig childLocationConfig : locationConfig.getChildList()) {

                // check if given child is registered otherwise register.
                if (!registry.contains(childLocationConfig)) {
                    logger.warn("registered child[" + childLocationConfig + "] for parent[" + locationConfig + "] does not exists.");
                    throw new EntryModification(registry.register(childLocationConfig), this);
                }

                IdentifiableMessage<String, LocationConfig, LocationConfig.Builder> child = entryMap.get(childLocationConfig, registry.getIdGenerator());
                // check if parent id is registered
                if (!childLocationConfig.hasParentId()) {
                    throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setParentId(locationConfig.getId())), this);
                }

                // check if parent id is valid.
                if (!childLocationConfig.getParentId().equals(locationConfig.getId())) {
                    throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setParentId(locationConfig.getId())), this);
                }

                // check if the position of a child has been changed in the parent location
                // TODO:critical because every time the value in the parent is set for the child. What is if the child is updated?
                if (!childLocationConfig.getPosition().equals(child.getMessage().getPosition())) {
                    throw new EntryModification(child.setMessage(child.getMessage().toBuilder().setPosition(childLocationConfig.getPosition())), this);
                }
            }
        } finally { //sync children with registry
            if (!locationConfig.getChildList().isEmpty()) {
                List<LocationConfig> updatedChildrenList = new ArrayList<>();
                for (LocationConfig childLocationConfig : locationConfig.getChildList()) {
                    updatedChildrenList.add(entryMap.get(childLocationConfig, registry.getIdGenerator()).getMessage());
                }
                entry.setMessage(locationConfig.toBuilder().clearChild().addAllChild(updatedChildrenList).build());
            }
        }
    }

    private boolean parentHasChild(LocationConfig parent, LocationConfig child) {
        for (LocationConfig children : parent.getChildList()) {
            if (children.hasId() && children.getId().equals(child.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reset() {
    }
}
