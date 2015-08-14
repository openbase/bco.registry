/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dm.core.consistency;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.container.ProtoBufMessageMapInterface;
import de.citec.jul.storage.registry.EntryModification;
import de.citec.jul.storage.registry.ProtoBufRegistryConsistencyHandler;
import de.citec.jul.storage.registry.ProtoBufRegistryInterface;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class UnitTemplateValidationConsistencyHandler implements ProtoBufRegistryConsistencyHandler<String, UnitTemplate, UnitTemplate.Builder> {

    @Override
    public void processData(String id, IdentifiableMessage<String, UnitTemplate, UnitTemplate.Builder> entry, ProtoBufMessageMapInterface<String, UnitTemplate, UnitTemplate.Builder> entryMap, ProtoBufRegistryInterface<String, UnitTemplate, UnitTemplate.Builder> registry) throws CouldNotPerformException, EntryModification {
        UnitTemplate.Builder unitTemplate = entry.getMessage().toBuilder();
        
        // remove invalid unit template
        if(!unitTemplate.getId().equals(registry.getIdGenerator().generateId(unitTemplate.build()))) {
            registry.remove(entry);
            throw new EntryModification(entry, this);
        }
        
        String templateId;
        UnitTemplate template;
        
        // create missing unit template
        if(registry.size() != UnitTemplate.UnitType.values().length) {
            for(UnitType unitType : UnitType.values()) {
                template = UnitTemplate.newBuilder().setType(unitType).build();
                templateId = registry.getIdGenerator().generateId(template);
                if(!registry.contains(templateId)) {
                    registry.register(template);
                    throw new EntryModification(template, this); 
                }
            }
        }
    }

    @Override
    public void reset() {
    }
}